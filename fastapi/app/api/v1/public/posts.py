"""
Public Posts API
인증이 필요없는 포스트 조회 API
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Optional

from app.core.database import get_db
from app.schemas import (
    PostResponse,
    PostListResponse,
    PostSearchResponse,
    SearchResultResponse,
)
from app.services.post_service import PostService

router = APIRouter(prefix="/posts", tags=["public-posts"])


@router.get("", response_model=PostListResponse)
async def list_posts(
    skip: int = 0,
    limit: int = 20,
    blog_id: Optional[int] = None,
    db: AsyncSession = Depends(get_db)
):
    """
    포스트 목록 조회 (Public)

    - **skip**: 건너뛸 개수
    - **limit**: 최대 조회 개수
    - **blog_id**: 특정 블로그의 포스트만 조회 (optional)
    """
    service = PostService(db)
    posts, total = await service.get_posts(
        skip=skip,
        limit=limit,
        blog_id=blog_id,
        include_blog=True
    )

    return PostListResponse(total=total, posts=posts)


@router.get("/search", response_model=SearchResultResponse)
async def search_posts(
    q: str = Query(..., min_length=1, description="검색어"),
    limit: int = Query(20, ge=1, le=100, description="최대 결과 개수"),
    offset: int = Query(0, ge=0, description="건너뛸 개수"),
    db: AsyncSession = Depends(get_db)
):
    """
    포스트 전문 검색 (Public)

    - **q**: 검색어 (필수)
    - **limit**: 최대 결과 개수 (1-100, 기본값 20)
    - **offset**: 건너뛸 개수 (페이지네이션)

    PostgreSQL Full-Text Search를 사용하여 title과 content에서 검색합니다.
    결과는 연관도(rank) 순으로 정렬됩니다.
    """
    service = PostService(db)
    search_results, total = await service.search_posts(
        query=q,
        limit=limit,
        offset=offset
    )

    # 결과를 PostSearchResponse로 변환
    results = [PostSearchResponse(**result) for result in search_results]

    return SearchResultResponse(
        total=total,
        results=results
    )


@router.get("/{post_id}", response_model=PostResponse)
async def get_post(
    post_id: int,
    db: AsyncSession = Depends(get_db)
):
    """특정 포스트 조회 (Public, blog 정보 포함)"""
    service = PostService(db)
    post = await service.get_post_by_id(post_id, include_blog=True)

    if not post:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Post with id {post_id} not found"
        )

    return post
