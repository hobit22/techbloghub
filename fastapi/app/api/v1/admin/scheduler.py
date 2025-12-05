"""
Admin Scheduler API
RSS 수집, 콘텐츠 처리, 재시도 등을 세밀하게 제어 (Admin 전용)
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
import logging
from typing import Optional

from app.core.database import get_db
from app.core.auth import verify_admin_key
from app.models import Blog, Post
from app.services.rss_collector import RSSCollector
from app.services.content_processor import ContentProcessor

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/admin/scheduler",
    tags=["admin-scheduler"],
    dependencies=[Depends(verify_admin_key)]
)


# ==================== RSS 수집 ====================

@router.post("/rss-collect")
async def collect_all_rss(
    db: AsyncSession = Depends(get_db)
):
    """
    모든 활성 블로그의 RSS 수집 (Admin)

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    try:
        collector = RSSCollector(db)
        results = await collector.collect_all_active_blogs()

        total_new = sum(r['new_posts'] for r in results)
        total_skipped = sum(r['skipped_duplicates'] for r in results)
        total_errors = sum(len(r['errors']) for r in results)

        return {
            "status": "success",
            "summary": {
                "blogs_processed": len(results),
                "new_posts": total_new,
                "skipped_duplicates": total_skipped,
                "errors": total_errors
            },
            "details": results
        }
    except Exception as e:
        logger.error(f"RSS collection failed: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@router.post("/rss-collect/{blog_id}")
async def collect_blog_rss(
    blog_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    특정 블로그의 RSS 수집 (Admin)

    - **blog_id**: 수집할 블로그 ID

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    # 블로그 존재 확인
    result = await db.execute(select(Blog).where(Blog.id == blog_id))
    blog = result.scalar_one_or_none()

    if not blog:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Blog with id {blog_id} not found"
        )

    try:
        collector = RSSCollector(db)
        result = await collector.collect_blog(blog)

        return {
            "status": "success",
            "blog_id": blog_id,
            "blog_name": blog.name,
            "summary": {
                "new_posts": result['new_posts'],
                "skipped_duplicates": result['skipped_duplicates'],
                "errors": len(result['errors'])
            },
            "errors": result['errors']
        }
    except Exception as e:
        logger.error(f"RSS collection failed for blog {blog_id}: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


# ==================== 콘텐츠 처리 ====================

@router.post("/content-process")
async def process_content_batch(
    batch_size: int = Query(50, ge=1, le=200, description="배치 크기 (1-200)"),
    db: AsyncSession = Depends(get_db)
):
    """
    PENDING 상태 포스트를 배치로 처리 (Admin)

    - **batch_size**: 한 번에 처리할 포스트 개수 (기본: 50, 최대: 200)

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    try:
        processor = ContentProcessor(db)
        summary = await processor.process_pending_batch(batch_size=batch_size)

        return {
            "status": "success",
            "summary": summary
        }
    except Exception as e:
        logger.error(f"Content processing failed: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@router.post("/content-process/{post_id}")
async def process_single_post(
    post_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    특정 포스트의 콘텐츠 처리 (Admin)

    - **post_id**: 처리할 포스트 ID

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    # 포스트 존재 확인
    result = await db.execute(select(Post).where(Post.id == post_id))
    post = result.scalar_one_or_none()

    if not post:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Post with id {post_id} not found"
        )

    try:
        processor = ContentProcessor(db)
        result = await processor.process_post(post)

        # DB에 변경사항 커밋
        await db.commit()
        await db.refresh(post)

        return {
            "status": "success",
            "post_id": post_id,
            "result": result,
            "post_status": post.status.value,
            "content_length": len(post.content) if post.content else 0
        }
    except Exception as e:
        logger.error(f"Content processing failed for post {post_id}: {e}", exc_info=True)
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


# ==================== 실패 재처리 ====================

@router.post("/retry-failed")
async def retry_failed_posts(
    batch_size: int = Query(10, ge=1, le=100, description="재시도할 포스트 개수 (1-100)"),
    db: AsyncSession = Depends(get_db)
):
    """
    FAILED 상태 포스트 재시도 (Admin)

    - **batch_size**: 한 번에 재시도할 포스트 개수 (기본: 10, 최대: 100)

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    try:
        processor = ContentProcessor(db)
        summary = await processor.retry_failed_posts(batch_size=batch_size)

        return {
            "status": "success",
            "summary": summary
        }
    except Exception as e:
        logger.error(f"Retry failed posts processing failed: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


# ==================== 통계 ====================

@router.get("/stats")
async def get_processing_stats(
    db: AsyncSession = Depends(get_db)
):
    """
    현재 처리 상태 통계 조회 (Admin)

    포스트 상태별 개수, 블로그 통계 등을 조회합니다.

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    try:
        processor = ContentProcessor(db)
        stats = await processor.get_processing_stats()

        # 블로그 통계 추가
        from app.models.blog import BlogStatus
        from sqlalchemy import func

        # 활성 블로그 수
        active_blogs_result = await db.execute(
            select(func.count(Blog.id)).where(Blog.status == BlogStatus.ACTIVE)
        )
        active_blogs = active_blogs_result.scalar()

        # 전체 블로그 수
        total_blogs_result = await db.execute(select(func.count(Blog.id)))
        total_blogs = total_blogs_result.scalar()

        return {
            "status": "success",
            "post_stats": stats,
            "blog_stats": {
                "total": total_blogs,
                "active": active_blogs,
                "inactive": total_blogs - active_blogs
            }
        }
    except Exception as e:
        logger.error(f"Failed to get stats: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


# ==================== 특정 블로그의 포스트 처리 ====================

@router.post("/content-process/blog/{blog_id}")
async def process_blog_posts(
    blog_id: int,
    batch_size: int = Query(50, ge=1, le=200, description="배치 크기 (1-200)"),
    db: AsyncSession = Depends(get_db)
):
    """
    특정 블로그의 PENDING 포스트만 처리 (Admin)

    - **blog_id**: 처리할 블로그 ID
    - **batch_size**: 한 번에 처리할 포스트 개수 (기본: 50, 최대: 200)

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    # 블로그 존재 확인
    blog_result = await db.execute(select(Blog).where(Blog.id == blog_id))
    blog = blog_result.scalar_one_or_none()

    if not blog:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Blog with id {blog_id} not found"
        )

    try:
        from app.models.post import PostStatus

        # 해당 블로그의 PENDING 포스트 조회
        result = await db.execute(
            select(Post)
            .where(
                Post.blog_id == blog_id,
                Post.status == PostStatus.PENDING,
                Post.retry_count < 3
            )
            .order_by(Post.created_at.asc())
            .limit(batch_size)
        )
        posts = result.scalars().all()

        if not posts:
            return {
                "status": "success",
                "blog_id": blog_id,
                "blog_name": blog.name,
                "message": "No pending posts to process",
                "summary": {
                    "total_processed": 0,
                    "completed": 0,
                    "failed": 0
                }
            }

        # 포스트 처리
        processor = ContentProcessor(db)
        completed = 0
        failed = 0
        errors = []

        for post in posts:
            result = await processor.process_post(post)
            if result['success']:
                completed += 1
            else:
                failed += 1
                errors.append({
                    'post_id': post.id,
                    'error': result.get('error', 'Unknown error')
                })

        await db.commit()

        return {
            "status": "success",
            "blog_id": blog_id,
            "blog_name": blog.name,
            "summary": {
                "total_processed": len(posts),
                "completed": completed,
                "failed": failed,
                "errors": errors
            }
        }
    except Exception as e:
        logger.error(f"Content processing failed for blog {blog_id}: {e}", exc_info=True)
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )
