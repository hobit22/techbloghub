from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import declarative_base

from app.core.config import settings

# SQLAlchemy 엔진 생성 (비동기)
engine = create_async_engine(
    settings.DATABASE_URL,
    echo=settings.DEBUG,  # SQL 쿼리 로깅 (개발시 True)
    future=True,
)

# 세션 팩토리
AsyncSessionLocal = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
)

# 모델 베이스 클래스
Base = declarative_base()


# Dependency: DB 세션 제공
async def get_db():
    """
    FastAPI dependency로 사용
    각 요청마다 DB 세션을 생성하고 종료

    사용 예시:
        @app.get("/posts")
        async def get_posts(db: AsyncSession = Depends(get_db)):
            ...
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()
