# FIVA AI Agent（Python LangChain 版）

从 Java 后端迁移出来的 AI 服务，负责**对话 Agent（`/api/chat/*`）**与**税负 AI（`/api/tax/suggestion*`、`/api/tax/analysis`）**。
技术栈：FastAPI + LangChain（langchain-openai 直连 DeepSeek）+ LangGraph（ReAct 工具循环）+ SQLAlchemy/PyMySQL（直连与 Java 后端相同的 MySQL 库）。

发票 OCR（`/api/invoice/*`）与税负计算（`/api/tax/calculate`）仍在 Java 后端。

## 接口契约（与 Java 版完全一致）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/chat/send` | 阻塞式对话 → `{sessionId, role, content}` |
| POST | `/api/chat/stream` | 流式对话 SSE：`session` → `token`×N → `done([DONE])` / `error` |
| GET | `/api/chat/history?sessionId=` | 历史消息 |
| DELETE | `/api/chat/history?sessionId=` | 清空会话 |
| GET | `/api/chat/metrics` | AI 调用指标（累计/成功率/平均延迟/今日Token/7天趋势） |
| POST | `/api/tax/suggestion` | 阻塞式税负建议 → `{success, data}` |
| POST | `/api/tax/suggestion/stream` | 流式税负建议（SSE 120s） |
| POST | `/api/tax/analysis` | 流式税负深度分析（SSE 180s） |

认证：与 Java 后端相同的 JWT（HS256，共享 `jwt.secret`），`Authorization: Bearer <token>`。

## 本地启动

```bash
cd ai-agent
py -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt
# 配置 .env（参考 .env.example，与 application-dev.properties 一致）
.venv\Scripts\python -m uvicorn app.main:app --host 0.0.0.0 --port 8001
```

健康检查：http://localhost:8001/health

## 目录结构

```
ai-agent/
├── app/
│   ├── main.py            # FastAPI 入口（CORS 与 Java 一致）
│   ├── config.py          # .env 配置
│   ├── auth.py            # JWT 校验（与 Java JwtUtil 同 secret）
│   ├── db.py              # SQLAlchemy + PyMySQL
│   ├── sse.py             # SSE 编码（与 Spring SseEmitter 逐字节兼容）
│   ├── agent/
│   │   ├── prompts.py     # 系统提示词（逐字复制自 ChatService）
│   │   ├── tools.py       # 8 个只读账务查询工具（SQL/文案与 FivaDataTools 一致）
│   │   ├── memory.py      # 会话记忆（TTL 30min / 200 会话 / 20 条窗口，DB 种子化）
│   │   └── chat_agent.py  # langgraph create_react_agent
│   ├── routers/           # chat / tax 路由
│   └── services/          # 编排：落库、埋点、错误脱敏
├── tests/                 # 纯逻辑单测（pytest）
├── requirements.txt
└── .env                   # 本地真实配置（不入库）
```

## 已知行为对齐说明

- SSE 多行 token 按 Spring 行为拆成多条 `data:` 行（2026-11 优化后前端解析器遵循标准 SSE 语义：同一事件的多行 data 以 `\n` 连接，流式过程中 markdown 可实时渲染）。
- 流式超时（对话 120s / 建议 120s / 分析 180s）与 Java `SseEmitter` 一致，表现为静默断流。
- 对话轮次成功后写 `t_chat_message`，AI 调用写 `t_ai_call_log`（与 Java 版共用同一套表，指标页面无感切换）。
- 客户端提前断开时，后台继续完成本轮落库与埋点（与 Java 异步回调行为一致）。
