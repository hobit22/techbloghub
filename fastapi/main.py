from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.api.v1.public import blogs as public_blogs
from app.api.v1.public import posts as public_posts
from app.api.v1.public import summaries as public_summaries
from app.api.v1.admin import blogs as admin_blogs
from app.api.v1.admin import posts as admin_posts
from app.api.v1.admin import scheduler as admin_scheduler
from app.scheduler import start_scheduler, shutdown_scheduler

# FastAPI 앱 생성
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    debug=settings.DEBUG,
    description="TechBlog Hub API - Public API와 Admin API로 구성됨",
)

# CORS 설정 (프론트엔드 연동)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Public API 라우터 등록 (인증 불필요)
app.include_router(public_blogs.router, prefix="/api/v1")
app.include_router(public_posts.router, prefix="/api/v1")
app.include_router(public_summaries.router, prefix="/api/v1")

# Admin API 라우터 등록 (X-Admin-Key 헤더 필요)
app.include_router(admin_blogs.router, prefix="/api/v1")
app.include_router(admin_posts.router, prefix="/api/v1")
app.include_router(admin_scheduler.router, prefix="/api/v1")


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
    from app.services.discord_notifier import discord_notifier

    print(f"🚀 {settings.APP_NAME} v{settings.APP_VERSION} 시작!")
    print(f"📊 Database: {settings.DATABASE_URL.split('@')[1]}")

    # 스케줄러 시작
    start_scheduler()
    print("⏰ Scheduler started (Asia/Seoul):")
    print("   - RSS Collection: Daily at 01:00 AM KST")
    print("   - Content Processing: Daily at 02:00 AM KST")
    print("   - Retry Failed Posts: Daily at 03:00 AM KST")

    # Discord 시작 알림
    await discord_notifier.notify_scheduler_start()


# 서버 종료 이벤트
@app.on_event("shutdown")
async def shutdown():
    print("👋 서버 종료 중...")
    shutdown_scheduler()
