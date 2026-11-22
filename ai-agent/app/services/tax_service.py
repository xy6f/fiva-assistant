"""税负 AI 服务：复刻 Java TaxService 的提示词与流式逻辑（建议/深度分析），发票 OCR 不在此处。"""
import asyncio
import logging
import time
from decimal import Decimal, ROUND_HALF_UP

from langchain_core.messages import AIMessageChunk, HumanMessage
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, ConfigDict
from starlette.concurrency import run_in_threadpool  # noqa: F401

from app.config import settings
from app.sse import encode_event
from app.services.chat_service import record_ai_call

logger = logging.getLogger(__name__)

TYPE_TAX_SUGGEST = "TAX_SUGGEST"
TYPE_TAX_ANALYSIS = "TAX_ANALYSIS"

_SUGGESTION_PROMPT_TEMPLATE = """你是一个专业的税务筹划顾问。请根据以下企业税务数据，给出3-5条合理的税务筹划建议。

企业数据：
- 年营业收入：{annual_revenue} 元
- 年营业成本：{annual_cost} 元
- 年期间费用：{annual_expenses} 元
- 纳税人类型：{taxpayer_type}
- 应纳增值税：{vat_payable} 元
- 应纳企业所得税：{income_tax_payable} 元
- 综合税负率：{tax_burden_rate}%
- 净利润率：{profit_margin}%

请给出具体、可操作的税务筹划建议，包括：
1. 可以享受的税收优惠政策
2. 合理的成本费用规划建议
3. 发票管理优化建议
4. 其他合法节税方案

用简洁的中文回答，每条建议不超过100字。
"""

_ANALYSIS_PROMPT_TEMPLATE = """你是一个资深税务筹划专家，请根据以下企业税务数据，生成一份完整的税务深度分析报告。

企业数据：
- 年营业收入：{annual_revenue} 元
- 年营业成本：{annual_cost} 元
- 年期间费用：{annual_expenses} 元
- 员工人数：{employee_count} 人
- 纳税人类型：{taxpayer_type}
- 应纳增值税：{vat_payable} 元
- 应纳企业所得税：{income_tax_payable} 元
- 优惠减免金额：{income_tax_discount} 元
- 综合税负率：{tax_burden_rate}%
- 净利润：{net_profit} 元
- 净利润率：{profit_margin}%

请从以下维度进行分析，用专业、清晰的中文回答：

## 一、税负健康度评估
评估企业当前税负水平是否合理，与同行业平均水平对比分析。

## 二、税收优惠政策利用分析
分析企业是否充分利用了现有的税收优惠政策，还有哪些可以享受但尚未利用的。

## 三、税务风险预警
识别潜在的税务风险点，如税负率异常、成本费用比例不合理等。

## 四、税务筹划优化方案
给出3-5条具体、可操作的税务优化建议，每条建议需包含：
- 具体措施
- 预计节税金额或比例
- 实施难度和注意事项

## 五、年度税务规划建议
从全年角度给出税务规划的整体思路和时间节点建议。

注意：
- 所有分析需基于真实税法规定，以最新政策为准则
- 建议要具体可操作，不要泛泛而谈
- 涉及金额的地方请给出估算
"""


class TaxCalculation(BaseModel):
    """与 Java dto.TaxCalculation 字段一一对应（camelCase）。"""
    model_config = ConfigDict(extra="ignore")

    annualRevenue: Decimal | None = None
    annualCost: Decimal | None = None
    annualExpenses: Decimal | None = None
    employeeCount: int = 0
    smallScale: bool | None = None
    vatRate: Decimal | None = None
    vatPayable: Decimal | None = None
    vatWithInputDeduction: Decimal | None = None
    vatExemptAmount: Decimal | None = None
    urbanMaintenanceTax: Decimal | None = None
    educationSurcharge: Decimal | None = None
    localEducationSurcharge: Decimal | None = None
    taxableIncome: Decimal | None = None
    incomeTaxRate: Decimal | None = None
    incomeTaxPayable: Decimal | None = None
    incomeTaxDiscount: Decimal | None = None
    totalTaxBurden: Decimal | None = None
    taxBurdenRate: Decimal | None = None
    netProfit: Decimal | None = None
    profitMargin: Decimal | None = None
    suggestion: str | None = None


def _f2(value: Decimal | None) -> str:
    """等价 Java String.format("%.2f", v)：两位小数、HALF_UP；null 输出 "null"（同 Java Formatter）。"""
    if value is None:
        return "null"
    return str(value.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))


def _taxpayer_type(calc: TaxCalculation) -> str:
    return "小规模纳税人" if calc.smallScale is True else "一般纳税人"


def build_suggestion_prompt(calc: TaxCalculation) -> str:
    return _SUGGESTION_PROMPT_TEMPLATE.format(
        annual_revenue=_f2(calc.annualRevenue),
        annual_cost=_f2(calc.annualCost),
        annual_expenses=_f2(calc.annualExpenses),
        taxpayer_type=_taxpayer_type(calc),
        vat_payable=_f2(calc.vatPayable),
        income_tax_payable=_f2(calc.incomeTaxPayable),
        tax_burden_rate=_f2(calc.taxBurdenRate),
        profit_margin=_f2(calc.profitMargin),
    )


def build_analysis_prompt(calc: TaxCalculation) -> str:
    return _ANALYSIS_PROMPT_TEMPLATE.format(
        annual_revenue=_f2(calc.annualRevenue),
        annual_cost=_f2(calc.annualCost),
        annual_expenses=_f2(calc.annualExpenses),
        employee_count=calc.employeeCount,
        taxpayer_type=_taxpayer_type(calc),
        vat_payable=_f2(calc.vatPayable),
        income_tax_payable=_f2(calc.incomeTaxPayable),
        income_tax_discount=_f2(calc.incomeTaxDiscount),
        tax_burden_rate=_f2(calc.taxBurdenRate),
        net_profit=_f2(calc.netProfit),
        profit_margin=_f2(calc.profitMargin),
    )


_llm: ChatOpenAI | None = None


def _get_tax_llm() -> ChatOpenAI:
    """无工具的单次调用模型（对齐 Java TaxService 注入的 streamingChatModel）。"""
    global _llm
    if _llm is None:
        _llm = ChatOpenAI(
            model=settings.deepseek_model,
            api_key=settings.deepseek_api_key,
            base_url=settings.deepseek_base_url,
            temperature=None,
            timeout=settings.tax_analysis_timeout_seconds,
            max_retries=2,
            streaming=True,
            stream_usage=True,
        )
    return _llm


def _chunk_text(chunk) -> str:
    content = getattr(chunk, "content", "")
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        return "".join(
            block.get("text", "")
            for block in content
            if isinstance(block, dict) and block.get("type") == "text"
        )
    return ""


async def get_ai_suggestion(user_id: int, calc: TaxCalculation) -> str:
    """阻塞式 AI 税务筹划建议（对齐 Java getAiSuggestion：失败返回兜底文案，不抛异常）。"""
    prompt = build_suggestion_prompt(calc)
    try:
        response = await _get_tax_llm().ainvoke([HumanMessage(content=prompt)])
        return _chunk_text(response)
    except Exception:
        logger.exception("Failed to get AI suggestion")
        return "AI建议获取失败，请稍后重试。"


async def _produce_tax_stream(user_id: int, prompt: str, call_type: str,
                              timeout_seconds: int, queue: asyncio.Queue) -> None:
    """后台生产者：流式调用模型并推送 token 事件（对齐 Java streamToEmitter）。"""
    start_ms = time.monotonic()
    full: list[str] = []
    usage: dict | None = None
    try:
        async with asyncio.timeout(timeout_seconds):
            async for chunk in _get_tax_llm().astream([HumanMessage(content=prompt)]):
                if not isinstance(chunk, AIMessageChunk):
                    continue
                text = _chunk_text(chunk)
                if text:
                    full.append(text)
                    await queue.put(("token", text))
                if getattr(chunk, "usage_metadata", None):
                    usage = chunk.usage_metadata
        record_ai_call(
            user_id, call_type,
            usage.get("input_tokens") if usage else None,
            usage.get("output_tokens") if usage else None,
            int((time.monotonic() - start_ms) * 1000), True, None,
        )
        await queue.put(("done", "[DONE]"))
    except TimeoutError:
        record_ai_call(user_id, call_type, None, None,
                       int((time.monotonic() - start_ms) * 1000), False, "timeout")
        # Java SseEmitter 超时表现为静默断流，这里保持一致
        await queue.put(("done", "[DONE]"))
    except asyncio.CancelledError:
        raise
    except Exception as exc:
        logger.error("Tax AI stream error: %s", exc)
        record_ai_call(user_id, call_type, None, None,
                       int((time.monotonic() - start_ms) * 1000), False, str(exc))
        # Java 税负流的 error 事件发送原始错误信息
        await queue.put(("error", str(exc) or "AI 服务异常，请稍后重试"))
    finally:
        await queue.put(None)


async def stream_tax_events(user_id: int, prompt: str, call_type: str, timeout_seconds: int):
    """SSE 事件生成器：token(多次) -> done / error（无 session 事件，与 Java 税负流一致）。"""
    queue: asyncio.Queue = asyncio.Queue()
    producer = asyncio.create_task(_produce_tax_stream(user_id, prompt, call_type, timeout_seconds, queue))
    try:
        while True:
            item = await queue.get()
            if item is None:
                break
            kind, data = item
            yield encode_event(kind, data)
    finally:
        # 客户端断开时不取消 producer：让后台继续完成埋点（对齐 Java 行为）
        if producer.done():
            pass
