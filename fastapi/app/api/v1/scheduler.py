"""
스케줄러 관리 API
수동 트리거 및 상태 조회
"""

from fastapi import APIRouter, Depends, BackgroundTasks
from sqlalchemy.ext.asyncio import AsyncSession
import logging

from app.core.database import get_db
from app.services.rss_collector import RSSCollector
from app.services.content_processor import ContentProcessor

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/scheduler", tags=["scheduler"])


async def _run_rss_collection(db: AsyncSession):
    """백그라운드에서 RSS 수집 실행"""
    try:
        logger.info("RSS collection started in background")
        collector = RSSCollector(db)
        results = await collector.collect_all_active_blogs()

        total_new = sum(r['new_posts'] for r in results)
        total_skipped = sum(r['skipped_duplicates'] for r in results)
        total_errors = sum(len(r['errors']) for r in results)

        logger.info(f"RSS collection completed: {total_new} new, {total_skipped} skipped, {total_errors} errors")
    except Exception as e:
        logger.error(f"RSS collection failed: {e}", exc_info=True)
    finally:
        await db.close()


@router.post("/rss-collect/trigger")
async def trigger_rss_collection(
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db)
):
    """
    RSS 수집 작업 수동 트리거
    (백그라운드에서 비동기 실행, 즉시 응답 반환)
    """
    # 백그라운드 태스크로 실행
    background_tasks.add_task(_run_rss_collection, db)

    return {
        "status": "triggered",
        "message": "RSS collection started in background. Check logs for progress."
    }


async def _run_content_processing(db: AsyncSession, batch_size: int):
    """백그라운드에서 본문 추출 실행"""
    try:
        logger.info(f"Content processing started in background (batch_size={batch_size})")
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

        logger.info(f"Content processing completed: {total_summary['total_processed']} processed, "
                   f"{total_summary['completed']} completed, {total_summary['failed']} failed")
    except Exception as e:
        logger.error(f"Content processing failed: {e}", exc_info=True)
    finally:
        await db.close()


@router.post("/content-process/trigger")
async def trigger_content_processing(
    background_tasks: BackgroundTasks,
    batch_size: int = 50,
    db: AsyncSession = Depends(get_db)
):
    """
    본문 추출 작업 수동 트리거
    (백그라운드에서 비동기 실행, 즉시 응답 반환)
    """
    background_tasks.add_task(_run_content_processing, db, batch_size)

    return {
        "status": "triggered",
        "message": f"Content processing started in background (batch_size={batch_size}). Check logs for progress."
    }


async def _run_retry_failed(db: AsyncSession, batch_size: int):
    """백그라운드에서 실패 포스트 재시도 실행"""
    try:
        logger.info(f"Retry failed posts started in background (batch_size={batch_size})")
        processor = ContentProcessor(db)
        summary = await processor.retry_failed_posts(batch_size=batch_size)
        logger.info(f"Retry failed completed: {summary}")
    except Exception as e:
        logger.error(f"Retry failed processing failed: {e}", exc_info=True)
    finally:
        await db.close()


@router.post("/retry-failed/trigger")
async def trigger_retry_failed(
    background_tasks: BackgroundTasks,
    batch_size: int = 10,
    db: AsyncSession = Depends(get_db)
):
    """
    실패 포스트 재시도 수동 트리거
    (백그라운드에서 비동기 실행, 즉시 응답 반환)
    """
    background_tasks.add_task(_run_retry_failed, db, batch_size)

    return {
        "status": "triggered",
        "message": f"Retry failed processing started in background (batch_size={batch_size}). Check logs for progress."
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
