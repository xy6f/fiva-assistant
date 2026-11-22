"""对话路由：与 Java ChatController 完全相同的 URL、参数与响应契约。"""
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app.auth import get_current_user_id
from app.services import chat_service

router = APIRouter(prefix="/api/chat", tags=["chat"])


class ChatRequest(BaseModel):
    """与 Java dto.ChatRequest 对齐（未知字段忽略，等价 Jackson 默认行为）。"""
    sessionId: str | None = None
    message: str | None = None


def _validate(request: ChatRequest) -> None:
    error = chat_service.validate_chat_message(request.message)
    if error:
        raise HTTPException(status_code=400, detail=error)


@router.post("/send")
async def send(request: ChatRequest, user_id: int = Depends(get_current_user_id)):
    """阻塞式对话：POST /api/chat/send -> {sessionId, role, content}"""
    _validate(request)
    return await chat_service.chat_blocking(user_id, request.sessionId, request.message)


@router.post("/stream")
async def stream(request: ChatRequest, user_id: int = Depends(get_current_user_id)):
    """流式对话：POST /api/chat/stream -> text/event-stream"""
    _validate(request)
    return StreamingResponse(
        chat_service.stream_chat_events(user_id, request.sessionId, request.message),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


@router.get("/history")
def get_history(sessionId: str, user_id: int = Depends(get_current_user_id)):
    return chat_service.get_history(sessionId, user_id)


@router.delete("/history")
def clear_history(sessionId: str, user_id: int = Depends(get_current_user_id)):
    chat_service.clear_history(sessionId, user_id)
    return {"success": True}


@router.get("/metrics")
def get_metrics(user_id: int = Depends(get_current_user_id)):
    return chat_service.get_metrics(user_id)
