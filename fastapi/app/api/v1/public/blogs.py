"""
Public Blogs API
인증이 필요없는 블로그 조회 API
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from typing import List

from app.core.database import get_db
from app.schemas import BlogResponse, BlogListResponse
from app.services.blog_service import BlogService

router = APIRouter(prefix="/blogs", tags=["public-blogs"])


@router.get("/", response_model=BlogListResponse)
async def list_blogs(
    skip: int = 0,
    limit: int = 20,
    db: AsyncSession = Depends(get_db)
):
    """
    블로그 목록 조회 (Public)

    - **skip**: 건너뛸 개수 (pagination)
    - **limit**: 최대 조회 개수 (기본 20)
    """
    service = BlogService(db)
    blogs, total = await service.get_blogs(skip=skip, limit=limit)

    return BlogListResponse(total=total, blogs=blogs)


@router.get("/active", response_model=List[BlogResponse])
async def get_active_blogs(
    db: AsyncSession = Depends(get_db)
):
    """
    활성 상태의 블로그 목록 조회 (Public)

    - 상태가 ACTIVE인 블로그만 반환
    """
    service = BlogService(db)
    blogs = await service.get_active_blogs()

    return blogs


@router.get("/{blog_id}", response_model=BlogResponse)
async def get_blog(
    blog_id: int,
    db: AsyncSession = Depends(get_db)
):
    """특정 블로그 조회 (Public)"""
    service = BlogService(db)
    blog = await service.get_blog_by_id(blog_id)

    if not blog:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Blog with id {blog_id} not found"
        )

    return blog
