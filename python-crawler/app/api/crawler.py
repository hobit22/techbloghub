from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import Dict, Any
from loguru import logger

from ..core.database import get_db
from ..services import CrawlerService

router = APIRouter(prefix="/crawler", tags=["crawler"])


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