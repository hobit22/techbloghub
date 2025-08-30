from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session
from typing import Dict, Any
from loguru import logger

from ..core.database import get_db
from ..services.crawler_service import CrawlerService

router = APIRouter(prefix="/crawler", tags=["crawler"])


@router.get("/health")
async def health_check():
    """헬스 체크 엔드포인트"""
    return {"status": "healthy", "service": "RSS Crawler"}


@router.get("/status")
async def get_crawling_status(db: Session = Depends(get_db)):
    """크롤링 상태 조회"""
    try:
        crawler_service = CrawlerService(db)
        status = await crawler_service.get_crawling_status()
        
        # 태깅 상태 정보도 포함
        tagging_status = crawler_service.get_tagging_status()
        status.update(tagging_status)
        
        return status
    except Exception as e:
        logger.error(f"Error getting crawling status: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/blogs/{blog_id}")
async def crawl_specific_blog(
    blog_id: int,
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """특정 블로그 크롤링"""
    try:
        crawler_service = CrawlerService(db)
        saved_count = await crawler_service.crawl_specific_blog(blog_id)
        
        return {
            "blog_id": blog_id,
            "posts_saved": saved_count,
            "message": f"Successfully crawled blog {blog_id}, saved {saved_count} posts"
        }
        
    except ValueError as e:
        logger.warning(f"Blog not found or invalid: {e}")
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        logger.error(f"Error crawling blog {blog_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/blogs/all")
async def crawl_all_blogs(
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """모든 활성화된 블로그 크롤링 (백그라운드 작업)"""
    try:
        # 즉시 응답 반환하고 백그라운드에서 크롤링 실행
        background_tasks.add_task(run_crawl_all_blogs, db)
        
        return {
            "message": "Crawling started for all active blogs",
            "status": "started"
        }
        
    except Exception as e:
        logger.error(f"Error starting crawl all blogs: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/blogs/all/sync")
async def crawl_all_blogs_sync(db: Session = Depends(get_db)) -> Dict[str, Any]:
    """모든 활성화된 블로그 크롤링 (동기 실행)"""
    try:
        crawler_service = CrawlerService(db)
        result = await crawler_service.crawl_all_active_blogs()
        return result
        
    except Exception as e:
        logger.error(f"Error crawling all blogs: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def run_crawl_all_blogs(db: Session):
    """백그라운드 작업용 크롤링 함수"""
    try:
        logger.info("Starting background crawl for all active blogs")
        crawler_service = CrawlerService(db)
        result = await crawler_service.crawl_all_active_blogs()
        logger.info(f"Background crawl completed: {result}")
        
    except Exception as e:
        logger.error(f"Error in background crawl: {e}")
    finally:
        db.close()


@router.post("/tagging/enable")
async def enable_tagging(db: Session = Depends(get_db)):
    """크롤링 시 태깅 활성화"""
    try:
        crawler_service = CrawlerService(db)
        crawler_service.set_tagging_enabled(True)
        return {
            "message": "Tagging enabled for crawling",
            "tagging_enabled": True
        }
    except Exception as e:
        logger.error(f"Error enabling tagging: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/tagging/disable")
async def disable_tagging(db: Session = Depends(get_db)):
    """크롤링 시 태깅 비활성화"""
    try:
        crawler_service = CrawlerService(db)
        crawler_service.set_tagging_enabled(False)
        return {
            "message": "Tagging disabled for crawling",
            "tagging_enabled": False
        }
    except Exception as e:
        logger.error(f"Error disabling tagging: {e}")
        raise HTTPException(status_code=500, detail=str(e))