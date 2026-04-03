"""
Public Posts API
인증이 필요없는 포스트 조회 API
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Optional, List

from app.core.database import get_db
from app.schemas import (
    PostResponse,
    PostListResponse,
    PostSearchResponse,
    SearchResultResponse,
)
from app.services import PostService

router = APIRouter(prefix="/posts", tags=["public-posts"])


@router.get("", response_model=PostListResponse)
async def list_posts(
    skip: int = 0,
    limit: int = 20,
    blog_ids: Optional[List[int]] = Query(
        None, description="블로그 ID 필터 (복수 선택 가능)"
    ),
    db: AsyncSession = Depends(get_db),
):
    """
    포스트 목록 조회 (Public)

    - **skip**: 건너뛸 개수
    - **limit**: 최대 조회 개수
    - **blog_ids**: 특정 블로그들의 포스트만 조회 (복수 선택 가능, optional)
    """
    service = PostService(db)
    posts, total = await service.get_posts(
        skip=skip, limit=limit, blog_ids=blog_ids, include_blog=True
    )

    return PostListResponse(total=total, posts=posts)


@router.get("/search", response_model=SearchResultResponse)
async def search_posts(
    q: str = Query(..., min_length=1, description="검색어"),
    limit: int = Query(20, ge=1, le=100, description="최대 결과 개수"),
    offset: int = Query(0, ge=0, description="건너뛸 개수"),
    blog_ids: Optional[List[int]] = Query(
        None, description="블로그 ID 필터 (복수 선택 가능)"
    ),
    db: AsyncSession = Depends(get_db),
):
    service = PostService(db)
    posts_with_rank, total = await service.search_posts(
        query=q, limit=limit, offset=offset, blog_ids=blog_ids
    )

    results = []
    for post, rank in posts_with_rank:
        post_data = PostResponse.model_validate(post).model_dump()
        results.append(PostSearchResponse(**post_data, rank=rank))

    return SearchResultResponse(total=total, results=results)


@router.get("/{post_id}", response_model=PostResponse)
async def get_post(post_id: int, db: AsyncSession = Depends(get_db)):
    """특정 포스트 조회 (Public, blog 정보 포함)"""
    service = PostService(db)
    post = await service.get_post_by_id(post_id, include_blog=True)

    if not post:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Post with id {post_id} not found",
        )

    return post
