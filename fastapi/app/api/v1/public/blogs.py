"""
Public Blogs API
인증이 필요없는 블로그 조회 API
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from typing import List

from app.core.database import get_db
from app.models import Blog
from app.schemas import BlogResponse, BlogListResponse

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
    # 총 개수 조회
    total_result = await db.execute(select(func.count(Blog.id)))
    total = total_result.scalar()

    # 블로그 목록 조회
    result = await db.execute(
        select(Blog)
        .order_by(Blog.created_at.desc())
        .offset(skip)
        .limit(limit)
    )
    blogs = result.scalars().all()

    return BlogListResponse(total=total, blogs=blogs)


@router.get("/active", response_model=List[BlogResponse])
async def get_active_blogs(
    db: AsyncSession = Depends(get_db)
):
    """
    활성 상태의 블로그 목록 조회 (Public)

    - 상태가 ACTIVE인 블로그만 반환
    """
    from app.models.blog import BlogStatus

    result = await db.execute(
        select(Blog)
        .where(Blog.status == BlogStatus.ACTIVE)
        .order_by(Blog.created_at.desc())
    )
    blogs = result.scalars().all()

    return blogs


@router.get("/{blog_id}", response_model=BlogResponse)
async def get_blog(
    blog_id: int,
    db: AsyncSession = Depends(get_db)
):
    """특정 블로그 조회 (Public)"""
    result = await db.execute(select(Blog).where(Blog.id == blog_id))
    blog = result.scalar_one_or_none()

    if not blog:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Blog with id {blog_id} not found"
        )

    return blog
