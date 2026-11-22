"""税负 AI 路由：与 Java TaxController 的 AI 端点契约一致（/calculate 仍由 Java 提供）。"""
from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse

from app.auth import get_current_user_id
from app.config import settings
from app.services import tax_service
from app.services.tax_service import TaxCalculation

router = APIRouter(prefix="/api/tax", tags=["tax"])


@router.post("/suggestion")
async def suggestion(calc: TaxCalculation, user_id: int = Depends(get_current_user_id)):
    """阻塞式 AI 筹划建议：{success: true, data: "..."}"""
    suggestion_text = await tax_service.get_ai_suggestion(user_id, calc)
    return {"success": True, "data": suggestion_text}


@router.post("/suggestion/stream")
async def suggestion_stream(calc: TaxCalculation, user_id: int = Depends(get_current_user_id)):
    """流式 AI 筹划建议（SSE，120s 超时，与 Java SseEmitter 一致）。"""
    prompt = tax_service.build_suggestion_prompt(calc)
    return StreamingResponse(
        tax_service.stream_tax_events(user_id, prompt, tax_service.TYPE_TAX_SUGGEST,
                                      settings.tax_suggest_timeout_seconds),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.post("/analysis")
async def analysis(calc: TaxCalculation, user_id: int = Depends(get_current_user_id)):
    """流式税务深度分析报告（SSE，180s 超时，与 Java SseEmitter 一致）。"""
    prompt = tax_service.build_analysis_prompt(calc)
    return StreamingResponse(
        tax_service.stream_tax_events(user_id, prompt, tax_service.TYPE_TAX_ANALYSIS,
                                      settings.tax_analysis_timeout_seconds),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
