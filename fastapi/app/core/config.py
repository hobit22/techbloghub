from pydantic_settings import BaseSettings
from pydantic import field_validator
import json


class Settings(BaseSettings):
    """
    애플리케이션 설정
    .env 파일에서 자동으로 읽어옴
    """

    # 앱 기본 설정
    APP_NAME: str = "TechBlog Hub"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False

    # 데이터베이스 설정
    DATABASE_URL: str

    # CORS 설정 (프론트엔드 연동)
    ALLOWED_ORIGINS: list[str] = ["http://localhost:3000", "http://localhost:8000"]

    # Admin API 인증
    ADMIN_API_KEY: str = "your-secret-admin-key-change-in-production"

    # RSS 크롤링 설정
    RSS_PROXY_URL: str

    # 컨텐츠 추출 설정
    MIN_CONTENT_LENGTH: int = 500
    MIN_TEXT_RATIO: float = 0.01
    PLAYWRIGHT_TIMEOUT: int = 30000

    # OpenAI 설정
    OPENAI_API_KEY: str
    OPENAI_MODEL: str = "gpt-4o-mini"
    OPENAI_MAX_TOKENS: int = 10000

    # Discord 알림 설정
    DISCORD_WEBHOOK_URL: str = ""
    DISCORD_WEBHOOK_ENABLED: bool = False

    # Elasticsearch 설정
    ELASTICSEARCH_URL: str = "http://localhost:9200"

    class Config:
        env_file = ".env"
        case_sensitive = True


# 싱글톤 인스턴스
_settings = None


def get_settings() -> Settings:
    """설정 가져오기 (싱글톤 패턴)"""
    global _settings
    if _settings is None:
        _settings = Settings()
    return _settings


settings = get_settings()
