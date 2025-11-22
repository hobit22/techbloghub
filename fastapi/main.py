from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings

# FastAPI 앱 생성x
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    debug=settings.DEBUG,
)

# CORS 설정 (프론트엔드 연동)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
async def root():
    """루트 엔드포인트 - 서버 상태 확인"""
    return {
        "message": "Welcome to TechBlog Hub API",
        "version": settings.APP_VERSION,
        "status": "running"
    }


@app.get("/health")
async def health_check():
    """헬스체크 엔드포인트"""
    return {"status": "healthy"}


# 서버 시작 이벤트
@app.on_event("startup")
async def startup():
    print(f"🚀 {settings.APP_NAME} v{settings.APP_VERSION} 시작!")
    print(f"📊 Database: {settings.DATABASE_URL.split('@')[1]}")


# 서버 종료 이벤트
@app.on_event("shutdown")
async def shutdown():
    print("👋 서버 종료 중...")
