from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import sys
import os
from loguru import logger
from dotenv import load_dotenv

# Load environment variables first
load_dotenv()

from .core.config import settings
from .core.database import engine
from .models import Base
from .api import crawler, backfill, tag, admin

# Configure logging
logger.remove()
logger.add(
    sys.stdout,
    level=settings.log_level.upper(),
    format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>"
)

# Create FastAPI app
app = FastAPI(
    title="TechBlogHub Crawler Service",
    version="1.0.0", 
    description="Simplified crawling and tagging service with core APIs only",
    openapi_url="/api/openapi.json",
    docs_url="/api/docs",
    redoc_url="/api/redoc"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 개발용, 프로덕션에서는 제한 필요
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(crawler.router, prefix="/api")
app.include_router(backfill.router, prefix="/api")
app.include_router(tag.router, prefix="/api")
app.include_router(admin.router, prefix="/api")

# Create database tables
Base.metadata.create_all(bind=engine)

# Log environment variable status on startup
logger.info(f"OpenAI API Key loaded: {'Yes' if settings.openai_api_key else 'No'}")
logger.info(f"Database URL: {settings.database_url}")


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "TechBlogHub Crawler Service",
        "version": "1.0.0",
        "description": "Core crawling and tagging APIs",
        "status": "running",
        "openai_configured": bool(settings.openai_api_key),
        "available_endpoints": {
            "crawling": [
                "POST /api/crawler/blogs/{blog_id} - Crawl specific blog",
                "POST /api/crawler/blogs/all/sync - Crawl all active blogs"
            ],
            "backfill": [
                "POST /api/backfill/{blog_id} - Backfill specific blog"
            ],
            "tagging": [
                "POST /api/tags/posts/{post_id}/auto-tag - Auto-tag specific post",
                "POST /api/tags/untagged/auto-tag?limit=N - Auto-tag N untagged posts (default: 10)"
            ],
            "admin": [
                "GET /api/admin/rejected-tags - List rejected tags",
                "GET /api/admin/rejected-tags/stats - Rejected tag statistics",
                "GET /api/admin/rejected-tags/candidates - Approval candidates",
                "POST /api/admin/rejected-tags/{tag_name}/approve - Approve rejected tag",
                "POST /api/admin/rejected-tags/batch-approve - Batch approve tags"
            ]
        }
    }


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)