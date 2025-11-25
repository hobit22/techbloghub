from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional


class PostBase(BaseModel):
    """포스트 기본 스키마"""
    title: str = Field(..., min_length=1, max_length=500, description="게시글 제목")
    content: Optional[str] = Field(None, description="게시글 본문")
    author: Optional[str] = Field(None, max_length=200, description="작성자")
    original_url: str = Field(..., description="원본 게시글 URL")
    blog_id: int = Field(..., description="블로그 ID")
    published_at: Optional[datetime] = Field(None, description="게시글 발행 시각")


class PostCreate(PostBase):
    """포스트 생성 요청 스키마"""
    pass


class PostUpdate(BaseModel):
    """포스트 수정 요청 스키마 (모든 필드 optional)"""
    title: Optional[str] = Field(None, min_length=1, max_length=500)
    content: Optional[str] = None
    author: Optional[str] = Field(None, max_length=200)
    published_at: Optional[datetime] = None


class PostResponse(PostBase):
    """포스트 응답 스키마"""
    id: int
    normalized_url: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    keywords: list[str] = Field(default_factory=list, description="추출된 키워드")

    class Config:
        from_attributes = True


class PostListResponse(BaseModel):
    """포스트 목록 응답 스키마"""
    total: int
    posts: list[PostResponse]


class PostSearchResponse(PostResponse):
    """포스트 검색 결과 스키마 (연관도 포함)"""
    rank: float = Field(..., description="검색 연관도 점수")


class SearchResultResponse(BaseModel):
    """검색 결과 응답 스키마"""
    total: int
    results: list[PostSearchResponse]
