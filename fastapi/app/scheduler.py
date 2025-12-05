"""
스케줄러 설정
APScheduler를 사용한 정기 작업 실행
"""

import logging
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger
from pytz import timezone

from app.core.database import AsyncSessionLocal
from app.services.rss_collector import RSSCollector
from app.services.content_processor import ContentProcessor
from app.services.discord_notifier import discord_notifier

logger = logging.getLogger(__name__)

# 한국 시간대 설정
KST = timezone('Asia/Seoul')

# 스케줄러 인스턴스 (timezone 설정)
scheduler = AsyncIOScheduler(timezone=KST)


async def collect_rss_job():
    """
    RSS 수집 작업
    매일 오전 1시에 실행: 새로운 URL만 수집
    """
    logger.info("=" * 50)
    logger.info("Starting RSS collection job")
    logger.info("=" * 50)

    async with AsyncSessionLocal() as db:
        try:
            collector = RSSCollector(db)
            results = await collector.collect_all_active_blogs()

            total_new = sum(r['new_posts'] for r in results)
            total_skipped = sum(r['skipped_duplicates'] for r in results)
            total_errors = sum(len(r['errors']) for r in results)

            logger.info(f"RSS collection completed:")
            logger.info(f"  - Blogs processed: {len(results)}")
            logger.info(f"  - New posts collected: {total_new}")
            logger.info(f"  - Duplicates skipped: {total_skipped}")
            logger.info(f"  - Errors: {total_errors}")

            # 상세 결과
            for result in results:
                if result['new_posts'] > 0 or result['errors']:
                    logger.info(
                        f"  [{result['blog_name']}] "
                        f"New: {result['new_posts']}, "
                        f"Skipped: {result['skipped_duplicates']}, "
                        f"Errors: {len(result['errors'])}"
                    )

            # Discord 알림 전송
            await discord_notifier.notify_rss_collection(results)

        except Exception as e:
            logger.error(f"RSS collection job failed: {e}", exc_info=True)
            # 에러 알림
            await discord_notifier.notify_error("RSS Collection", str(e))


async def process_content_job():
    """
    본문 추출 작업
    매일 오전 2시에 실행: PENDING 상태 포스트들의 본문 추출
    """
    logger.info("=" * 50)
    logger.info("Starting content processing job")
    logger.info("=" * 50)

    async with AsyncSessionLocal() as db:
        try:
            processor = ContentProcessor(db)

            # 통계 확인
            stats_before = await processor.get_processing_stats()
            logger.info(f"Status before processing: {stats_before}")

            # PENDING 포스트 처리 (배치 사이즈: 50)
            batch_size = 50
            total_summary = {
                'total_processed': 0,
                'completed': 0,
                'failed': 0,
                'errors': []
            }

            while True:
                summary = await processor.process_pending_batch(batch_size=batch_size)

                if summary['total_processed'] == 0:
                    break  # 더 이상 처리할 것이 없음

                # 누적
                total_summary['total_processed'] += summary['total_processed']
                total_summary['completed'] += summary['completed']
                total_summary['failed'] += summary['failed']
                total_summary['errors'].extend(summary['errors'])

                logger.info(
                    f"Batch processed: {summary['completed']} completed, "
                    f"{summary['failed']} failed"
                )

            # 최종 통계
            stats_after = await processor.get_processing_stats()
            logger.info(f"Content processing completed:")
            logger.info(f"  - Total processed: {total_summary['total_processed']}")
            logger.info(f"  - Completed: {total_summary['completed']}")
            logger.info(f"  - Failed: {total_summary['failed']}")
            logger.info(f"Status after processing: {stats_after}")

            # 에러 상세
            if total_summary['errors']:
                logger.warning(f"Errors occurred: {len(total_summary['errors'])}")
                for error in total_summary['errors'][:10]:  # 최대 10개만
                    logger.warning(f"  - Post {error['post_id']}: {error['error']}")

            # Discord 알림 전송
            await discord_notifier.notify_content_processing(total_summary)

        except Exception as e:
            logger.error(f"Content processing job failed: {e}", exc_info=True)
            # 에러 알림
            await discord_notifier.notify_error("Content Processing", str(e))


async def retry_failed_job():
    """
    실패 재시도 작업
    매일 오전 3시에 실행: FAILED 상태 포스트 재시도
    """
    logger.info("=" * 50)
    logger.info("Starting failed posts retry job")
    logger.info("=" * 50)

    async with AsyncSessionLocal() as db:
        try:
            processor = ContentProcessor(db)
            summary = await processor.retry_failed_posts(batch_size=10)

            logger.info(f"Failed posts retry completed:")
            logger.info(f"  - Total retried: {summary['total_retried']}")
            logger.info(f"  - Completed: {summary['completed']}")
            logger.info(f"  - Still failed: {summary['failed']}")

            if summary['errors']:
                logger.warning(f"Errors during retry: {len(summary['errors'])}")
                for error in summary['errors']:
                    logger.warning(
                        f"  - Post {error['post_id']} "
                        f"(retry #{error['retry_count']}): {error['error']}"
                    )

            # Discord 알림 전송
            await discord_notifier.notify_retry_failed(summary)

        except Exception as e:
            logger.error(f"Retry failed job failed: {e}", exc_info=True)
            # 에러 알림
            await discord_notifier.notify_error("Retry Failed Posts", str(e))


def start_scheduler():
    """스케줄러 시작"""

    # 1. RSS 수집: 매일 오전 1시 (KST)
    scheduler.add_job(
        collect_rss_job,
        trigger=CronTrigger(hour=1, minute=0, timezone=KST),
        id='collect_rss',
        name='RSS Collection Job',
        replace_existing=True
    )

    # 2. 본문 추출: 매일 오전 2시 (KST)
    scheduler.add_job(
        process_content_job,
        trigger=CronTrigger(hour=2, minute=0, timezone=KST),
        id='process_content',
        name='Content Processing Job',
        replace_existing=True
    )

    # 3. 실패 재시도: 매일 오전 3시 (KST)
    scheduler.add_job(
        retry_failed_job,
        trigger=CronTrigger(hour=3, minute=0, timezone=KST),
        id='retry_failed',
        name='Retry Failed Posts Job',
        replace_existing=True
    )

    scheduler.start()
    logger.info("Scheduler started successfully (Timezone: Asia/Seoul)")
    logger.info("Scheduled jobs:")
    for job in scheduler.get_jobs():
        logger.info(f"  - {job.name} (ID: {job.id}): {job.next_run_time}")


def shutdown_scheduler():
    """스케줄러 종료"""
    if scheduler.running:
        scheduler.shutdown()
        logger.info("Scheduler shut down")
