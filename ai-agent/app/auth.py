"""JWT 认证：与 Java 后端 JwtUtil 共用同一 HS256 secret，解析 claim 中的 userId。"""
import jwt
from fastapi import Depends, Header

from app.config import settings


def _unauthorized(detail: str) -> Exception:
    from fastapi import HTTPException
    return HTTPException(status_code=401, detail=detail)


def resolve_user_id(authorization: str | None) -> int:
    """校验 Bearer token 并返回 userId（等价 Java JwtAuthenticationFilter + UserContext）。"""
    if not authorization or not authorization.lower().startswith("bearer "):
        raise _unauthorized("未登录或登录已过期")
    token = authorization[7:].strip()
    try:
        # Java JJWT 会按密钥长度自动选择 HS256/384/512（jwt.secret 为 448bit 时签出 HS384），
        # 因此这里同时接受三种 HS* 算法，与 Java 签发的 token 兼容
        payload = jwt.decode(token, settings.jwt_secret, algorithms=["HS256", "HS384", "HS512"])
    except jwt.ExpiredSignatureError:
        raise _unauthorized("登录已过期")
    except jwt.InvalidTokenError:
        raise _unauthorized("无效的登录凭证")
    user_id = payload.get("userId")
    if user_id is None:
        raise _unauthorized("无效的登录凭证")
    return int(user_id)


def get_current_user_id(authorization: str | None = Header(default=None)) -> int:
    """FastAPI 依赖：从 Authorization 头解析当前用户。"""
    return resolve_user_id(authorization)


current_user = Depends(get_current_user_id)
