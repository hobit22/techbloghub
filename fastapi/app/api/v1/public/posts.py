"""
Public Posts API
인증이 필요없는 포스트 조회 API
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, text
from sqlalchemy.orm import selectinload
from typing import Optional

from app.core.database import get_db
from app.models import Post, Blog
from app.schemas import (
    PostResponse,
    PostListResponse,
    PostSearchResponse,
    SearchResultResponse,
)

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
    # 쿼리 빌드 (blog 정보 함께 로드)
    query = select(Post).options(selectinload(Post.blog))
    count_query = select(func.count(Post.id))

    if blog_id:
        query = query.where(Post.blog_id == blog_id)
        count_query = count_query.where(Post.blog_id == blog_id)

    # 총 개수
    total_result = await db.execute(count_query)
    total = total_result.scalar()

    # 포스트 목록
    result = await db.execute(
        query
        .order_by(Post.published_at.desc().nulls_last(), Post.created_at.desc())
        .offset(skip)
        .limit(limit)
    )
    posts = result.scalars().all()

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
    # 검색어를 tsquery로 변환 (simple 분석기)
    search_query = ' & '.join(q.strip().split())  # "fastapi tutorial" -> "fastapi & tutorial"

    # ts_rank를 사용한 전문 검색 쿼리 (blog 정보 포함)
    query = text("""
        SELECT
            posts.id, posts.title, posts.content, posts.author,
            posts.original_url, posts.normalized_url, posts.blog_id,
            posts.published_at, posts.created_at, posts.updated_at,
            blogs.id as blog_id, blogs.name as blog_name,
            blogs.company as blog_company, blogs.site_url as blog_site_url,
            blogs.logo_url as blog_logo_url,
            ts_rank(posts.keyword_vector, to_tsquery('simple', :search_query)) as rank
        FROM posts
        JOIN blogs ON posts.blog_id = blogs.id
        WHERE posts.keyword_vector @@ to_tsquery('simple', :search_query)
        ORDER BY rank DESC, posts.published_at DESC NULLS LAST
        LIMIT :limit
        OFFSET :offset
    """)

    # 검색 실행
    result = await db.execute(
        query,
        {
            "search_query": search_query,
            "limit": limit,
            "offset": offset
        }
    )
    rows = result.fetchall()

    # 총 개수 조회
    count_query = text("""
        SELECT COUNT(*) as total
        FROM posts
        WHERE posts.keyword_vector @@ to_tsquery('simple', :search_query)
    """)

    count_result = await db.execute(count_query, {"search_query": search_query})
    total = count_result.scalar()

    # 결과 변환
    from app.schemas.blog import BlogInfo
    search_results = []
    for row in rows:
        blog_info = BlogInfo(
            id=row.blog_id,
            name=row.blog_name,
            company=row.blog_company,
            site_url=row.blog_site_url,
            logo_url=row.blog_logo_url
        )

        post_dict = {
            "id": row.id,
            "title": row.title,
            "content": row.content,
            "author": row.author,
            "original_url": row.original_url,
            "normalized_url": row.normalized_url,
            "blog_id": row.blog_id,
            "published_at": row.published_at,
            "created_at": row.created_at,
            "updated_at": row.updated_at,
            "keywords": [],
            "blog": blog_info,
            "rank": float(row.rank)
        }
        search_results.append(PostSearchResponse(**post_dict))

    return SearchResultResponse(
        total=total or 0,
        results=search_results
    )


@router.get("/{post_id}", response_model=PostResponse)
async def get_post(
    post_id: int,
    db: AsyncSession = Depends(get_db)
):
    """특정 포스트 조회 (Public, blog 정보 포함)"""
    result = await db.execute(
        select(Post)
        .options(selectinload(Post.blog))
        .where(Post.id == post_id)
    )
    post = result.scalar_one_or_none()

    if not post:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Post with id {post_id} not found"
        )

    return post
