from pydantic import BaseModel, HttpUrl, Field
from datetime import datetime
from typing import Optional

from app.models.blog import BlogStatus, BlogType


class BlogBase(BaseModel):
    """블로그 기본 스키마"""
    name: str = Field(..., min_length=1, max_length=255, description="블로그 이름")
    company: str = Field(..., min_length=1, max_length=255, description="회사/개인 이름")
    description: Optional[str] = Field(None, max_length=500, description="블로그 설명")
    rss_url: str = Field(..., description="RSS 피드 URL")
    site_url: str = Field(..., description="블로그 사이트 URL")
    logo_url: Optional[str] = Field(None, description="로고 이미지 URL")
    blog_type: Optional[BlogType] = Field(BlogType.COMPANY, description="블로그 타입")


class BlogCreate(BlogBase):
    """블로그 생성 요청 스키마"""
    pass


class BlogUpdate(BaseModel):
    """블로그 수정 요청 스키마 (모든 필드 optional)"""
    name: Optional[str] = Field(None, min_length=1, max_length=255)
    company: Optional[str] = Field(None, min_length=1, max_length=255)
    description: Optional[str] = Field(None, max_length=500)
    rss_url: Optional[str] = None
    site_url: Optional[str] = None
    logo_url: Optional[str] = None
    status: Optional[BlogStatus] = None
    blog_type: Optional[BlogType] = None


class BlogResponse(BlogBase):
    """블로그 응답 스키마"""
    id: int
    status: BlogStatus
    last_crawled_at: Optional[datetime] = None
    failure_count: int = 0
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True  # SQLAlchemy 모델을 Pydantic으로 변환 가능


class BlogListResponse(BaseModel):
    """블로그 목록 응답 스키마"""
    total: int
    blogs: list[BlogResponse]
