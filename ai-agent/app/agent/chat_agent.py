"""Agent 编排：用 langgraph create_react_agent 替代 Java LangChain4j AiServices。

- ChatOpenAI 直连 DeepSeek OpenAI 兼容端点（与 Java AiConfig 同 base-url/model/key）
- 工具按 userId 构建并绑定（数据隔离），agent 按 userId 缓存
"""
import logging
import threading

from langchain_openai import ChatOpenAI

from app.config import settings
from app.agent.prompts import SYSTEM_PROMPT
from app.agent.tools import build_fiva_tools

logger = logging.getLogger(__name__)

_lock = threading.Lock()
_agents: dict[int, object] = {}


def get_agent(user_id: int):
    """获取（并缓存）绑定当前用户工具集的 react agent。"""
    agent = _agents.get(user_id)
    if agent is not None:
        return agent
    with _lock:
        agent = _agents.get(user_id)
        if agent is None:
            llm = ChatOpenAI(
                model=settings.deepseek_model,
                api_key=settings.deepseek_api_key,
                base_url=settings.deepseek_base_url,
                temperature=None,
                timeout=settings.chat_timeout_seconds,
                max_retries=2,
                streaming=True,
                stream_usage=True,
            )
            agent = create_react_agent_safely(llm, user_id)
            _agents[user_id] = agent
        return agent


def create_react_agent_safely(llm, user_id: int):
    """构建 react agent（兼容不同 langgraph 版本的 prompt 参数名）。"""
    from langgraph.prebuilt import create_react_agent

    try:
        return create_react_agent(llm, build_fiva_tools(user_id), prompt=SYSTEM_PROMPT)
    except TypeError:
        # 旧版 langgraph 使用 state_modifier 注入系统提示词
        return create_react_agent(llm, build_fiva_tools(user_id), state_modifier=SYSTEM_PROMPT)
