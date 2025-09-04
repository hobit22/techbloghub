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
from .api import crawler, backfill

# Configure logging
logger.remove()
logger.add(
    sys.stdout,
    level=settings.log_level.upper(),
    format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>"
)

# Create FastAPI app
app = FastAPI(
    title=settings.api_title,
    version=settings.api_version,
    description=settings.api_description,
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

# Create database tables
Base.metadata.create_all(bind=engine)

# Log environment variable status on startup
logger.info(f"OpenAI API Key loaded: {'Yes' if settings.openai_api_key else 'No'}")
logger.info(f"Database URL: {settings.database_url}")


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": settings.api_title,
        "version": settings.api_version,
        "status": "running",
        "openai_configured": bool(settings.openai_api_key)
    }


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)