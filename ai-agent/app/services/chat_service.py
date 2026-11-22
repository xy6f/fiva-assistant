"""对话服务：复刻 Java ChatService 的编排逻辑（记忆/落库/埋点/错误脱敏/SSE 事件序列）。

SSE 事件序列与 Java 版完全一致：
  event:session  ->  event:token(多次)  ->  event:done([DONE]) / event:error
"""
import asyncio
import logging
import math
import time
import uuid
from datetime import date, datetime, timedelta

from langchain_core.messages import AIMessage, AIMessageChunk, BaseMessage, HumanMessage
from starlette.concurrency import run_in_threadpool

from app import db
from app.agent import chat_agent
from app.agent.memory import memory_store
from app.config import settings
from app.sse import encode_event

logger = logging.getLogger(__name__)

TYPE_CHAT = "CHAT"
MAX_MESSAGE_LENGTH = 2000

_INSERT_CHAT_MESSAGE_SQL = (
    "INSERT INTO t_chat_message (user_id, session_id, role, content, created_at) "
    "VALUES (:user_id, :session_id, :role, :content, :created_at)"
)
_INSERT_AI_CALL_LOG_SQL = (
    "INSERT INTO t_ai_call_log (user_id, call_type, prompt_tokens, completion_tokens, "
    "latency_ms, success, error_msg) "
    "VALUES (:user_id, :call_type, :prompt_tokens, :completion_tokens, :latency_ms, :success, :error_msg)"
)
_HISTORY_SQL = (
    "SELECT * FROM t_chat_message WHERE session_id = :session_id AND user_id = :user_id "
    "ORDER BY created_at ASC"
)
_DELETE_HISTORY_SQL = (
    "DELETE FROM t_chat_message WHERE session_id = :session_id AND user_id = :user_id"
)
# 与 AiCallLogMapper.aggregateByUserId 相同的聚合
_METRICS_SQL = (
    "SELECT COUNT(*) AS totalCalls, "
    "SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS successCalls, "
    "AVG(latency_ms) AS avgLatencyMs, "
    "SUM(CASE WHEN DATE(created_at) = CURDATE() THEN prompt_tokens + completion_tokens ELSE 0 END) AS todayTokens "
    "FROM t_ai_call_log WHERE user_id = :user_id"
)
# 与 AiCallLogMapper.dailyTokenTrend 相同的聚合
_TREND_SQL = (
    "SELECT DATE_FORMAT(created_at, '%m-%d') AS day, SUM(prompt_tokens + completion_tokens) AS tokens "
    "FROM t_ai_call_log "
    "WHERE user_id = :user_id AND created_at >= CURDATE() - INTERVAL 6 DAY "
    "GROUP BY DATE_FORMAT(created_at, '%m-%d'), DATE(created_at) ORDER BY DATE(created_at)"
)


class ChatError(Exception):
    """业务错误：以 400 {error: BAD_REQUEST, message: ...} 返回（对齐 Java 全局异常处理）。"""


def validate_chat_message(message: str | None) -> str | None:
    """复刻 ChatController 的入参校验；返回错误文案或 None。"""
    if message is None or message.strip() == "":
        return "消息不能为空"
    if len(message) > MAX_MESSAGE_LENGTH:
        return f"消息长度不能超过{MAX_MESSAGE_LENGTH}字"
    return None


def sanitize_error_message(error: Exception) -> str:
    """脱敏错误信息（复刻 ChatService.sanitizeErrorMessage 的规则与顺序）。"""
    msg = str(error)
    if not msg:
        return "AI 服务异常，请稍后重试"
    if "api" in msg or "key" in msg or "auth" in msg:
        return "AI 服务认证异常，请联系管理员"
    if "SQL" in msg or "JDBC" in msg:
        return "数据查询异常，请稍后重试"
    if "timeout" in msg or "Timeout" in msg:
        return "AI 回复超时，请稍后重试"
    return "AI 服务异常，请稍后重试"


def _chunk_text(chunk) -> str:
    """从流式消息块中提取纯文本增量。"""
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


def _extract_ai_text(state_messages: list[BaseMessage]) -> str:
    """取最终 AI 回复文本（等价 Java fullResponse 拼接结果）。"""
    for msg in reversed(state_messages):
        if isinstance(msg, AIMessage):
            content = msg.content
            if isinstance(content, str):
                return content
            if isinstance(content, list):
                return "".join(
                    block.get("text", "")
                    for block in content
                    if isinstance(block, dict) and block.get("type") == "text"
                )
    return ""


def persist_turn(session_id: str, user_id: int, user_text: str, assistant_text: str) -> None:
    """保存一轮对话到数据库（成功后调用）。"""
    db.execute(_INSERT_CHAT_MESSAGE_SQL, {
        "user_id": user_id, "session_id": session_id, "role": "user",
        "content": user_text, "created_at": datetime.now(),
    })
    db.execute(_INSERT_CHAT_MESSAGE_SQL, {
        "user_id": user_id, "session_id": session_id, "role": "assistant",
        "content": assistant_text, "created_at": datetime.now(),
    })


def record_ai_call(user_id: int, call_type: str, prompt_tokens, completion_tokens,
                   latency_ms: int, success: bool, error_msg: str | None) -> None:
    """AI 调用埋点（吞掉一切异常，复刻 AiCallLogService.record）。"""
    try:
        db.execute(_INSERT_AI_CALL_LOG_SQL, {
            "user_id": user_id,
            "call_type": call_type,
            "prompt_tokens": int(prompt_tokens or 0),
            "completion_tokens": int(completion_tokens or 0),
            "latency_ms": int(min(latency_ms, 2147483647)),
            "success": 1 if success else 0,
            "error_msg": error_msg[:500] if error_msg else None,
        })
    except Exception as e:
        logger.warning("Failed to record AI call log: %s", e)


def get_history(session_id: str, user_id: int) -> list[dict]:
    """历史消息（字段与 Java ChatMessage JSON 对齐）。"""
    rows = db.fetch_all(_HISTORY_SQL, {"session_id": session_id, "user_id": user_id})
    return [
        {
            "id": row["id"],
            "userId": row["user_id"],
            "sessionId": row["session_id"],
            "role": row["role"],
            "content": row["content"],
            "createdAt": row["created_at"].isoformat() if row.get("created_at") else None,
        }
        for row in rows
    ]


def clear_history(session_id: str, user_id: int) -> None:
    db.execute(_DELETE_HISTORY_SQL, {"session_id": session_id, "user_id": user_id})
    memory_store.remove(session_id)


def get_metrics(user_id: int) -> dict:
    """AI 指标（复刻 AiCallLogService.getMetrics + getDailyTokenTrend，含成功率先四舍五入规则）。"""
    row = db.fetch_one(_METRICS_SQL, {"user_id": user_id}) or {}
    total_calls = int(row.get("totalCalls") or 0)
    success_calls = row.get("successCalls")
    avg_latency = row.get("avgLatencyMs")
    today_tokens = row.get("todayTokens")

    if success_calls is None:
        success_rate = 100.0
    elif total_calls > 0:
        # Java: Math.round(success * 1000.0 / total) / 10.0
        success_rate = math.floor(float(success_calls) * 1000.0 / total_calls + 0.5) / 10.0
    else:
        success_rate = 100.0

    trend_rows = db.fetch_all(_TREND_SQL, {"user_id": user_id})
    tokens_by_day = {r["day"]: int(r["tokens"] or 0) for r in trend_rows}
    today = date.today()
    token_trend = []
    for i in range(6, -1, -1):
        day = (today - timedelta(days=i)).strftime("%m-%d")
        token_trend.append({"day": day, "tokens": tokens_by_day.get(day, 0)})

    return {
        "totalCalls": total_calls,
        "successRate": success_rate,
        "avgLatencyMs": float(avg_latency) if avg_latency is not None else 0.0,
        "todayTokens": int(today_tokens or 0),
        "tokenTrend": token_trend,
    }


def _normalize_session_id(session_id: str | None) -> str:
    return session_id if session_id and session_id.strip() else str(uuid.uuid4())


# 工具名 -> 进度短标签(前端流式时拼"正在xx…",完成后拼"已查询: xx、xx")
TOOL_STATUS_TEXT = {
    "monthlyIncomeExpense": "本月收支",
    "yearlyIncomeExpense": "年度收支",
    "incomeExpenseBetween": "区间收支",
    "accountBalances": "科目余额",
    "recentVouchers": "最近凭证",
    "invoiceStats": "发票数据",
    "monthlyTrend": "月度收支趋势",
    "budgetUsage": "预算执行",
}
TOOL_STATUS_FALLBACK = "账务数据"


async def chat_blocking(user_id: int, session_id: str | None, message: str) -> dict:
    """阻塞式对话（对齐 Java ChatService.chat）。"""
    final_session_id = _normalize_session_id(session_id)
    start_ms = time.monotonic()
    input_messages = memory_store.get_messages(final_session_id, user_id) + [HumanMessage(content=message)]
    agent = chat_agent.get_agent(user_id)

    try:
        async with asyncio.timeout(settings.chat_timeout_seconds):
            final_state = await agent.ainvoke({"messages": input_messages})
    except TimeoutError:
        memory_store.remove(final_session_id)
        record_ai_call(user_id, TYPE_CHAT, None, None,
                       int((time.monotonic() - start_ms) * 1000), False, "timeout")
        raise ChatError("AI 回复超时，请稍后重试")
    except Exception as exc:
        logger.error("Chat AI error: %s", exc)
        memory_store.remove(final_session_id)
        record_ai_call(user_id, TYPE_CHAT, None, None,
                       int((time.monotonic() - start_ms) * 1000), False, str(exc))
        raise ChatError("AI 未返回内容，请稍后重试")

    state_messages = list(final_state["messages"])
    content = _extract_ai_text(state_messages)
    if not content.strip():
        record_ai_call(user_id, TYPE_CHAT, None, None,
                       int((time.monotonic() - start_ms) * 1000), False, "empty response")
        raise ChatError("AI 未返回内容，请稍后重试")

    usage = _extract_usage(state_messages)
    await run_in_threadpool(
        _commit_turn, final_session_id, user_id, message,
        input_messages, state_messages, content, usage, start_ms,
    )
    return {"sessionId": final_session_id, "role": "assistant", "content": content}


def _extract_usage(state_messages: list[BaseMessage]) -> dict | None:
    for msg in reversed(state_messages):
        usage = getattr(msg, "usage_metadata", None)
        if usage:
            return usage
    return None


def _commit_turn(session_id: str, user_id: int, user_text: str,
                 input_messages: list[BaseMessage], state_messages: list[BaseMessage],
                 content: str, usage: dict | None, start_ms: float) -> None:
    """成功回合收尾：并入记忆 → 埋点 → 落库（顺序与 Java onCompleteResponse 一致）。"""
    new_messages = state_messages[len(input_messages):]
    memory_store.append(session_id, [HumanMessage(content=user_text), *new_messages])
    latency = int((time.monotonic() - start_ms) * 1000)
    record_ai_call(
        user_id, TYPE_CHAT,
        usage.get("input_tokens") if usage else None,
        usage.get("output_tokens") if usage else None,
        latency, True, None,
    )
    persist_turn(session_id, user_id, user_text, content)


async def _produce_chat_turn(user_id: int, session_id: str, message: str,
                             queue: asyncio.Queue) -> None:
    """后台生产者：跑完整个 agent 回合并推送 token / 工具进度事件。

    客户端提前断开时本任务继续执行，保证与 Java 一样完成落库与埋点。
    """
    start_ms = time.monotonic()
    full: list[str] = []
    usage: dict | None = None
    announced_tools: set[str] = set()
    try:
        input_messages = memory_store.get_messages(session_id, user_id) + [HumanMessage(content=message)]
        agent = chat_agent.get_agent(user_id)
        final_state = None
        async with asyncio.timeout(settings.chat_timeout_seconds):
            async for mode, payload in agent.astream(
                {"messages": input_messages}, stream_mode=["messages", "values"],
            ):
                if mode == "messages":
                    chunk = payload[0] if isinstance(payload, tuple) else payload
                    if not isinstance(chunk, AIMessageChunk):
                        continue
                    if getattr(chunk, "tool_call_chunks", None):
                        # 工具调用阶段：把工具名实时转成进度文案推送给前端
                        for tc in chunk.tool_call_chunks:
                            name = tc.get("name") if isinstance(tc, dict) else getattr(tc, "name", None)
                            if name and name not in announced_tools:
                                announced_tools.add(name)
                                status = TOOL_STATUS_TEXT.get(name, TOOL_STATUS_FALLBACK)
                                await queue.put(("tool", status))
                        continue
                    text = _chunk_text(chunk)
                    if text:
                        full.append(text)
                        await queue.put(("token", text))
                    if getattr(chunk, "usage_metadata", None):
                        usage = chunk.usage_metadata
                elif mode == "values":
                    final_state = payload

        state_messages = list(final_state["messages"]) if final_state else []
        content = "".join(full) or _extract_ai_text(state_messages)
        await run_in_threadpool(
            _commit_turn, session_id, user_id, message,
            input_messages, state_messages, content, usage, start_ms,
        )
        await queue.put(("done", "[DONE]"))
    except TimeoutError:
        memory_store.remove(session_id)
        record_ai_call(user_id, TYPE_CHAT, None, None,
                       int((time.monotonic() - start_ms) * 1000), False, "timeout")
        # Java SseEmitter 超时表现为静默断流，这里保持一致
        await queue.put(("done", "[DONE]"))
    except asyncio.CancelledError:
        raise
    except Exception as exc:
        logger.error("Chat AI stream error: %s", exc)
        memory_store.remove(session_id)
        record_ai_call(user_id, TYPE_CHAT, None, None,
                       int((time.monotonic() - start_ms) * 1000), False, str(exc))
        await queue.put(("error", sanitize_error_message(exc)))
    finally:
        await queue.put(None)


async def stream_chat_events(user_id: int, session_id: str | None, message: str):
    """SSE 事件生成器（对齐 Java ChatService.chatStream）。"""
    final_session_id = _normalize_session_id(session_id)
    yield encode_event("session", final_session_id)

    queue: asyncio.Queue = asyncio.Queue()
    producer = asyncio.create_task(_produce_chat_turn(user_id, final_session_id, message, queue))
    try:
        while True:
            item = await queue.get()
            if item is None:
                break
            kind, data = item
            yield encode_event(kind, data)
    finally:
        # 客户端断开时不取消 producer：让后台继续完成落库（对齐 Java 行为）
        if producer.done():
            pass
