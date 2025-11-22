from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """
    애플리케이션 설정
    .env 파일에서 자동으로 읽어옴
    """

    # 앱 기본 설정
    APP_NAME: str = "TechBlog Hub"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True

    # 데이터베이스 설정 (새로 만든 DB 사용)
    DATABASE_URL: str = "postgresql+asyncpg://admin:password@localhost:5432/techbloghub_fastapi"

    # CORS 설정 (프론트엔드 연동)
    ALLOWED_ORIGINS: list[str] = ["http://localhost:3000", "http://localhost:8000"]

    class Config:
        env_file = ".env"
        case_sensitive = True


# 싱글톤 인스턴스
settings = Settings()
