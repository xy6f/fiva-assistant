"""FIVA AI Agent 服务入口（Python LangChain 版）。

接管原 Java 后端中的对话 Agent 与税负 AI 接口，前端协议保持不变：
  POST /api/chat/send            阻塞式对话
  POST /api/chat/stream          流式对话（SSE）
  GET  /api/chat/history         历史消息
  DELETE /api/chat/history       清空会话
  GET  /api/chat/metrics         AI 调用指标
  POST /api/tax/suggestion       阻塞式税负建议
  POST /api/tax/suggestion/stream 流式税负建议（SSE）
  POST /api/tax/analysis         流式税负深度分析（SSE）
"""
import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.routers import chat, tax
from app.services.chat_service import ChatError

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")

app = FastAPI(title="FIVA AI Agent", version="1.0.0")

# 与 Java CorsConfigurationSource 一致
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173", "http://localhost:5174",
        "http://127.0.0.1:5173", "http://127.0.0.1:5174",
    ],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["*"],
    max_age=3600,
)


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """统一错误格式 {error: ...}，与项目前端 err.response.data.error 的读取方式对齐。"""
    detail = exc.detail
    if isinstance(detail, dict):
        return JSONResponse(status_code=exc.status_code, content=detail)
    return JSONResponse(status_code=exc.status_code, content={"error": str(detail)})


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=400,
        content={"error": "BAD_REQUEST", "message": "参数校验失败"},
    )


@app.exception_handler(ChatError)
async def chat_error_handler(request: Request, exc: ChatError):
    """对齐 Java GlobalExceptionHandler.handleRuntime：400 {error: BAD_REQUEST, message: ...}。"""
    return JSONResponse(
        status_code=400,
        content={"error": "BAD_REQUEST", "message": str(exc) or "服务器内部错误，请稍后重试"},
    )


app.include_router(chat.router)
app.include_router(tax.router)


@app.get("/health")
def health():
    return {"status": "ok"}
