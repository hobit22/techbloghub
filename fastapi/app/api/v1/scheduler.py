"""
스케줄러 관리 API
수동 트리거 및 상태 조회
"""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.services.rss_collector import RSSCollector
from app.services.content_processor import ContentProcessor

router = APIRouter(prefix="/scheduler", tags=["scheduler"])


@router.post("/rss-collect/trigger")
async def trigger_rss_collection(db: AsyncSession = Depends(get_db)):
    """
    RSS 수집 작업 수동 트리거
    (스케줄 대기 없이 즉시 실행)
    """
    collector = RSSCollector(db)
    results = await collector.collect_all_active_blogs()

    total_new = sum(r['new_posts'] for r in results)
    total_skipped = sum(r['skipped_duplicates'] for r in results)
    total_errors = sum(len(r['errors']) for r in results)

    return {
        "status": "completed",
        "summary": {
            "blogs_processed": len(results),
            "new_posts": total_new,
            "duplicates_skipped": total_skipped,
            "errors": total_errors
        },
        "details": results
    }


@router.post("/content-process/trigger")
async def trigger_content_processing(
    batch_size: int = 50,
    db: AsyncSession = Depends(get_db)
):
    """
    본문 추출 작업 수동 트리거
    (스케줄 대기 없이 즉시 실행)
    """
    processor = ContentProcessor(db)

    total_summary = {
        'total_processed': 0,
        'completed': 0,
        'failed': 0,
        'errors': []
    }

    # PENDING 포스트 배치 처리
    while True:
        summary = await processor.process_pending_batch(batch_size=batch_size)

        if summary['total_processed'] == 0:
            break

        total_summary['total_processed'] += summary['total_processed']
        total_summary['completed'] += summary['completed']
        total_summary['failed'] += summary['failed']
        total_summary['errors'].extend(summary['errors'])

    # 최종 통계
    stats = await processor.get_processing_stats()

    return {
        "status": "completed",
        "summary": total_summary,
        "current_stats": stats
    }


@router.post("/retry-failed/trigger")
async def trigger_retry_failed(
    batch_size: int = 10,
    db: AsyncSession = Depends(get_db)
):
    """
    실패 포스트 재시도 수동 트리거
    """
    processor = ContentProcessor(db)
    summary = await processor.retry_failed_posts(batch_size=batch_size)

    return {
        "status": "completed",
        "summary": summary
    }


@router.get("/stats")
async def get_processing_stats(db: AsyncSession = Depends(get_db)):
    """
    현재 처리 상태 통계 조회
    """
    processor = ContentProcessor(db)
    stats = await processor.get_processing_stats()

    return {
        "status": "success",
        "stats": stats
    }
