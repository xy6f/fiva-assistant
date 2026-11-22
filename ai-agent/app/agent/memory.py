"""会话记忆：复刻 Java ChatService 的内存记忆语义。

- key=sessionId 的内存缓存，TTL 30 分钟，最多 200 个会话（超限淘汰最久未访问）
- 首次访问时用 t_chat_message 的历史记录种子化（最近 20 条）
- 窗口上限 20 条；淘汰时保证窗口以用户消息开头，避免拆散 tool_calls/结果对
"""
import threading
import time

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage

from app import db

MEMORY_TTL_SECONDS = 30 * 60
MEMORY_MAX_ENTRIES = 200
MEMORY_MAX_MESSAGES = 20

# 与 ChatMessageMapper 相同的查询
_SEED_SQL = (
    "SELECT * FROM t_chat_message WHERE session_id = :session_id AND user_id = :user_id "
    "ORDER BY created_at ASC"
)


def load_seed_messages(session_id: str, user_id: int) -> list[BaseMessage]:
    """从 DB 历史记录种子化（复刻 loadHistoryAsLangChainMessages）。"""
    rows = db.fetch_all(_SEED_SQL, {"session_id": session_id, "user_id": user_id})
    messages: list[BaseMessage] = []
    for row in rows:
        role = row.get("role")
        content = row.get("content") or ""
        if role == "user":
            messages.append(HumanMessage(content=content))
        elif role == "assistant":
            messages.append(AIMessage(content=content))
    return messages[-MEMORY_MAX_MESSAGES:]


class SessionMemoryStore:
    """内存会话缓存（线程安全）。"""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._cache: dict[str, list[BaseMessage]] = {}
        self._access: dict[str, float] = {}

    def get_messages(self, session_id: str, user_id: int) -> list[BaseMessage]:
        """取会话消息列表；不存在时从 DB 种子化。返回共享列表，调用方在成功回合后负责追加。"""
        self._evict_stale()
        with self._lock:
            self._access[session_id] = time.time()
            messages = self._cache.get(session_id)
            if messages is None:
                if len(self._cache) >= MEMORY_MAX_ENTRIES:
                    self._evict_oldest_locked()
                messages = load_seed_messages(session_id, user_id)
                self._cache[session_id] = messages
            return messages

    def append(self, session_id: str, new_messages: list[BaseMessage]) -> None:
        """回合成功后把本轮消息并入记忆窗口（保证窗口以 user 消息开头）。"""
        with self._lock:
            messages = self._cache.get(session_id)
            if messages is None:
                return
            messages.extend(new_messages)
            trimmed = _trim_window(messages)
            self._cache[session_id] = trimmed

    def remove(self, session_id: str) -> None:
        """出错时清空该会话记忆，下次访问重新种子化（复刻 Java 行为）。"""
        with self._lock:
            self._cache.pop(session_id, None)
            self._access.pop(session_id, None)

    def _evict_stale(self) -> None:
        now = time.time()
        with self._lock:
            stale = [sid for sid, ts in self._access.items() if now - ts > MEMORY_TTL_SECONDS]
            for sid in stale:
                self._cache.pop(sid, None)
                self._access.pop(sid, None)

    def _evict_oldest_locked(self) -> None:
        if not self._access:
            return
        oldest = min(self._access, key=self._access.get)  # type: ignore[arg-type]
        self._cache.pop(oldest, None)
        self._access.pop(oldest, None)


def _trim_window(messages: list[BaseMessage]) -> list[BaseMessage]:
    """裁剪到最近 MEMORY_MAX_MESSAGES 条，且保证首条是 HumanMessage（工具消息不拆对）。"""
    trimmed = list(messages)
    while len(trimmed) > MEMORY_MAX_MESSAGES or (trimmed and not isinstance(trimmed[0], HumanMessage)):
        if not trimmed:
            break
        trimmed.pop(0)
    return trimmed


memory_store = SessionMemoryStore()
