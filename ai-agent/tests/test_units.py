"""纯逻辑单元测试：SSE 编码、错误脱敏、工具输出格式、指标聚合（不依赖数据库/LLM）。"""
import sys
from datetime import date
from decimal import Decimal
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.sse import encode_event
from app.services.chat_service import (
    get_metrics,
    sanitize_error_message,
    validate_chat_message,
)
from app.services import chat_service
from app.services.tax_service import TaxCalculation, build_suggestion_prompt
from app.agent import tools as tool_module


# ---------- SSE 编码 ----------

def test_sse_single_line():
    assert encode_event("token", "你好") == "event:token\ndata:你好\n\n"


def test_sse_multiline_splits_data_lines():
    # 与 Spring SseEmitter 一致：含换行的 data 拆成多行 data:
    assert encode_event("token", "a\nb") == "event:token\ndata:a\ndata:b\n\n"


def test_sse_done_event():
    assert encode_event("done", "[DONE]") == "event:done\ndata:[DONE]\n\n"


# ---------- 校验与脱敏 ----------

def test_validate_blank_message():
    assert validate_chat_message(None) == "消息不能为空"
    assert validate_chat_message("   ") == "消息不能为空"


def test_validate_too_long_message():
    assert validate_chat_message("好" * 2001) == "消息长度不能超过2000字"


def test_validate_ok():
    assert validate_chat_message("你好") is None


def test_sanitize_api_key_error():
    assert sanitize_error_message(Exception("401 Unauthorized: invalid api key")) == \
        "AI 服务认证异常，请联系管理员"


def test_sanitize_sql_error():
    assert sanitize_error_message(Exception("BadGrammar: SQL syntax error")) == \
        "数据查询异常，请稍后重试"


def test_sanitize_timeout_error():
    assert sanitize_error_message(Exception("request timeout after 120s")) == \
        "AI 回复超时，请稍后重试"


def test_sanitize_generic_error():
    assert sanitize_error_message(Exception("boom")) == "AI 服务异常，请稍后重试"


# ---------- 工具输出格式（打桩数据库访问） ----------

def test_monthly_income_expense_format(monkeypatch):
    def fake_fetch_one(sql, params):
        return {"total": Decimal("100.50") if params["type"] == "INCOME" else Decimal("30.25")}

    monkeypatch.setattr(tool_module.db, "fetch_one", fake_fetch_one)
    (monthly,) = [
        t for t in tool_module.build_fiva_tools(1)
        if t.name == "monthlyIncomeExpense"
    ]
    result = monthly.invoke({})
    assert result == "本月(%s年%s月)收入合计: 100.50 元, 支出合计: 30.25 元, 净额: 70.25 元" % (
        date.today().year, date.today().month)


def test_recent_vouchers_clamps_limit(monkeypatch):
    captured = {}

    def fake_fetch_all(sql, params):
        captured["limit_seen"] = True
        return [
            {"voucher_no": f"V{i}", "voucher_date": date(2026, 1, i % 28 + 1), "type": "INCOME",
             "amount": Decimal("10.00"), "description": None, "status": "APPROVED"}
            for i in range(100)
        ]

    monkeypatch.setattr(tool_module.db, "fetch_all", fake_fetch_all)
    (recent,) = [t for t in tool_module.build_fiva_tools(1) if t.name == "recentVouchers"]
    result = recent.invoke({"limit": 500})
    assert "共100张,显示50张" in result
    assert "V0 | 2026-01-01 | 收入 | 金额10.00元 |  | 状态:APPROVED" in result
    result_min = recent.invoke({"limit": 0})
    assert "显示1张" in result_min


def test_budget_usage_percent_format(monkeypatch):
    def fake_fetch_all(sql, params):
        return [{
            "year": 2026, "month": 1, "department": "销售部", "category": "差旅",
            "budget_amount": Decimal("300.00"), "used_amount": Decimal("200.00"),
        }]

    monkeypatch.setattr(tool_module.db, "fetch_all", fake_fetch_all)
    (budget,) = [t for t in tool_module.build_fiva_tools(1) if t.name == "budgetUsage"]
    result = budget.invoke({})
    # 200*100/300 = 66.67 -> HALF_UP 保留1位 -> 66.7（与 Java divide(scale=1, HALF_UP) 一致）
    assert "预算300.00元, 已用200.00元(66.7%)" in result


def test_account_balances_skips_zero(monkeypatch):
    def fake_fetch_all(sql, params):
        if "t_account" in sql:
            return [
                {"id": 1, "code": "1001", "name": "库存现金", "type": "ASSET"},
                {"id": 2, "code": "1002", "name": "银行存款", "type": "ASSET"},
            ]
        return [
            {"accountId": 1, "debit": Decimal("500.00"), "credit": Decimal("500.00")},
            {"accountId": 2, "debit": Decimal("800.00"), "credit": Decimal("300.00")},
        ]

    monkeypatch.setattr(tool_module.db, "fetch_all", fake_fetch_all)
    (accounts,) = [t for t in tool_module.build_fiva_tools(1) if t.name == "accountBalances"]
    result = accounts.invoke({})
    assert "1001 库存现金 [ASSET]: 0.00" not in result
    assert "1002 银行存款 [ASSET]: 500.00" in result


# ---------- 税负提示词 ----------

def test_suggestion_prompt_formatting():
    calc = TaxCalculation(
        annualRevenue=Decimal("1000000.005"), annualCost=Decimal("600000"), annualExpenses=Decimal("200000"),
        smallScale=False, vatPayable=Decimal("36000"), incomeTaxPayable=Decimal("50000"),
        taxBurdenRate=Decimal("8.6"), profitMargin=Decimal("15.0"),
    )
    prompt = build_suggestion_prompt(calc)
    assert "年营业收入：1000000.01 元" in prompt  # HALF_UP 两位小数
    assert "纳税人类型：一般纳税人" in prompt
    assert "综合税负率：8.60%" in prompt
    assert "%%" not in prompt


# ---------- 指标聚合 ----------

def test_metrics_success_rate_rounding(monkeypatch):
    """Java Math.round(x)/10：success 2/3 -> 66.7%（不是 Python round 的 66.7 vs 66.7 banker 差异场景）"""
    monkeypatch.setattr(chat_service.db, "fetch_one", lambda sql, params: {
        "totalCalls": 3, "successCalls": 2, "avgLatencyMs": 123.45, "todayTokens": 100,
    })
    monkeypatch.setattr(chat_service.db, "fetch_all", lambda sql, params: [])
    metrics = get_metrics(1)
    assert metrics["totalCalls"] == 3
    assert metrics["successRate"] == 66.7
    assert metrics["avgLatencyMs"] == 123.45
    assert metrics["todayTokens"] == 100
    assert len(metrics["tokenTrend"]) == 7


def test_metrics_empty_returns_defaults(monkeypatch):
    monkeypatch.setattr(chat_service.db, "fetch_one", lambda sql, params: None)
    monkeypatch.setattr(chat_service.db, "fetch_all", lambda sql, params: [])
    metrics = get_metrics(1)
    assert metrics["totalCalls"] == 0
    assert metrics["successRate"] == 100.0
    assert metrics["avgLatencyMs"] == 0.0
    assert metrics["todayTokens"] == 0
    assert len(metrics["tokenTrend"]) == 7
    assert all(item["tokens"] == 0 for item in metrics["tokenTrend"])
