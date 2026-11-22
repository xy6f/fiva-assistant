"""应用配置：从 ai-agent/.env 读取（对应 Java 侧 application-dev.properties 的 AI/JWT/DB 配置）。"""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # MySQL（与 Java 后端共用同一个库）
    db_host: str = "127.0.0.1"
    db_port: int = 3306
    db_user: str = "root"
    db_password: str = ""
    db_name: str = "fiva_assistant"

    # AI - DeepSeek（文本对话，与 Java AiConfig 的 ai.deepseek.* 等价）
    deepseek_base_url: str = "https://api.deepseek.com/v1"
    deepseek_api_key: str = "changeme"
    deepseek_model: str = "deepseek-v4-flash"

    # JWT（与 Java JwtUtil 共用同一 secret，HS256）
    jwt_secret: str = "changeme"

    # 服务
    app_host: str = "0.0.0.0"
    app_port: int = 8001

    # 流式超时（秒），与 Java SseEmitter 超时一致
    chat_timeout_seconds: int = 120
    tax_suggest_timeout_seconds: int = 120
    tax_analysis_timeout_seconds: int = 180


settings = Settings()
