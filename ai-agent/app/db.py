"""MySQL 访问层：SQLAlchemy + PyMySQL，与 Java 后端共用同一个 fiva_assistant 库。"""
from urllib.parse import quote_plus

from sqlalchemy import create_engine, text

from app.config import settings


def _database_url() -> str:
    return (
        f"mysql+pymysql://{settings.db_user}:{quote_plus(settings.db_password)}"
        f"@{settings.db_host}:{settings.db_port}/{settings.db_name}?charset=utf8mb4"
    )


engine = create_engine(_database_url(), pool_pre_ping=True, pool_recycle=3600, future=True)


def fetch_all(sql: str, params: dict | None = None) -> list[dict]:
    """执行查询，返回 dict 行（列名 -> 值）。"""
    with engine.connect() as conn:
        rows = conn.execute(text(sql), params or {}).mappings().all()
        return [dict(r) for r in rows]


def fetch_one(sql: str, params: dict | None = None) -> dict | None:
    """执行查询，返回第一行 dict 或 None。"""
    with engine.connect() as conn:
        row = conn.execute(text(sql), params or {}).mappings().first()
        return dict(row) if row is not None else None


def execute(sql: str, params: dict | None = None) -> None:
    """执行写语句并提交。"""
    with engine.begin() as conn:
        conn.execute(text(sql), params or {})
