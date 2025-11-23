"""
크롤링 API 엔드포인트
수동 크롤링 트리거 및 상태 조회
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from typing import List, Optional
from pydantic import BaseModel

from app.core.database import get_db
from app.models.blog import Blog
from app.services.rss_crawler import RSSCrawler


router = APIRouter(prefix="/crawler", tags=["crawler"])


# Response Models
class CrawlResult(BaseModel):
    """크롤링 결과"""
    blog_id: int
    blog_name: str
    total_urls: int
    new_posts: int
    failed_posts: int
    errors: List[str]

    class Config:
        from_attributes = True


class CrawlResponse(BaseModel):
    """크롤링 응답"""
    success: bool
    message: str
    results: List[CrawlResult]


class CrawlBlogRequest(BaseModel):
    """단일 블로그 크롤링 요청"""
    max_posts: Optional[int] = None


class CrawlAllBlogsRequest(BaseModel):
    """전체 블로그 크롤링 요청"""
    max_posts_per_blog: Optional[int] = None


@router.post("/blogs/{blog_id}/crawl", response_model=CrawlResponse)
async def crawl_single_blog(
    blog_id: int,
    request: CrawlBlogRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    특정 블로그 크롤링 (수동 트리거)

    - **blog_id**: 크롤링할 블로그 ID
    - **max_posts**: 최대 크롤링 개수 (None이면 전체)

    Returns:
        크롤링 결과 (새로 추가된 포스트 개수, 실패 개수 등)
    """
    # 블로그 조회
    result = await db.execute(select(Blog).where(Blog.id == blog_id))
    blog = result.scalar_one_or_none()

    if not blog:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Blog with id {blog_id} not found"
        )

    if not blog.is_active():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Blog '{blog.name}' is not active (status: {blog.status})"
        )

    # 크롤링 실행
    crawler = RSSCrawler(db)
    crawl_result = await crawler.crawl_blog(blog, max_posts=request.max_posts)

    return CrawlResponse(
        success=crawl_result['failed_posts'] == 0,
        message=f"Crawled {crawl_result['new_posts']} new posts from '{blog.name}'",
        results=[crawl_result]
    )


@router.post("/blogs/crawl-all", response_model=CrawlResponse)
async def crawl_all_blogs(
    request: CrawlAllBlogsRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    모든 활성 블로그 크롤링 (수동 트리거)

    - **max_posts_per_blog**: 블로그당 최대 크롤링 개수 (None이면 전체)

    Returns:
        모든 블로그의 크롤링 결과
    """
    crawler = RSSCrawler(db)
    results = await crawler.crawl_all_active_blogs(
        max_posts_per_blog=request.max_posts_per_blog
    )

    if not results:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No active blogs found"
        )

    total_new_posts = sum(r['new_posts'] for r in results)
    total_failed = sum(r['failed_posts'] for r in results)

    return CrawlResponse(
        success=total_failed == 0,
        message=f"Crawled {total_new_posts} new posts from {len(results)} blogs",
        results=results
    )


@router.post("/blogs/{blog_id}/test-rss", response_model=dict)
async def test_rss_extraction(
    blog_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    RSS 엔트리 추출 테스트 (본문 크롤링 없이 URL과 Title만 확인)

    - **blog_id**: 테스트할 블로그 ID

    Returns:
        {
            'blog_id': int,
            'blog_name': str,
            'rss_url': str,
            'total_entries': int,
            'sample_entries': List[dict]  # 최대 5개
        }
    """
    # 블로그 조회
    result = await db.execute(select(Blog).where(Blog.id == blog_id))
    blog = result.scalar_one_or_none()

    if not blog:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Blog with id {blog_id} not found"
        )

    # RSS 엔트리 추출 실행
    crawler = RSSCrawler(db)
    entries = crawler.extract_rss_entries(blog.rss_url)

    return {
        'blog_id': blog.id,
        'blog_name': blog.name,
        'rss_url': blog.rss_url,
        'total_entries': len(entries),
        'sample_entries': entries[:5]  # 최대 5개만
    }


@router.get("/stats", response_model=dict)
async def get_crawler_stats(
    db: AsyncSession = Depends(get_db)
):
    """
    크롤러 통계 조회

    Returns:
        {
            'total_blogs': int,
            'active_blogs': int,
            'inactive_blogs': int,
            'suspended_blogs': int,
            'total_posts': int
        }
    """
    from app.models.post import Post
    from sqlalchemy import func

    # 블로그 통계
    blog_stats = await db.execute(
        select(
            Blog.status,
            func.count(Blog.id).label('count')
        ).group_by(Blog.status)
    )

    stats = {
        'total_blogs': 0,
        'active_blogs': 0,
        'inactive_blogs': 0,
        'suspended_blogs': 0,
    }

    for status_val, count in blog_stats:
        stats['total_blogs'] += count
        if status_val == 'ACTIVE':
            stats['active_blogs'] = count
        elif status_val == 'INACTIVE':
            stats['inactive_blogs'] = count
        elif status_val == 'SUSPENDED':
            stats['suspended_blogs'] = count

    # 포스트 통계
    post_count = await db.execute(select(func.count(Post.id)))
    stats['total_posts'] = post_count.scalar()

    return stats
