from fastapi import APIRouter, HTTPException, BackgroundTasks, Depends
from loguru import logger
from typing import Dict, Any
from sqlalchemy.orm import Session

from ..core.database import get_db, SessionLocal
from ..services.crawling import CrawlerFactory
from ..models import BlogEntity

router = APIRouter(prefix="/backfill", tags=["backfill"])


@router.post("/{blog_id}")
def start_backfill(
    blog_id: int,
    background_tasks: BackgroundTasks,
    session: Session = Depends(get_db)
) -> Dict[str, Any]:
    """블로그 백필링 시작 - 블로그 타입에 따라 자동으로 적절한 크롤러 선택"""
    
    # 블로그 존재 확인
    blog = session.query(BlogEntity).filter(BlogEntity.id == blog_id).first()
    
    if not blog:
        raise HTTPException(status_code=404, detail=f"Blog with ID {blog_id} not found")
    
    # 블로그 타입 확인
    if not blog.blog_type:
        raise HTTPException(
            status_code=400, 
            detail=f"Blog {blog.name} does not have a blog_type set for backfilling"
        )
    
    # 백그라운드 태스크로 백필링 시작
    background_tasks.add_task(perform_backfill, blog_id)
    
    logger.info(f"Started {blog.blog_type} backfill for blog: {blog.name} (ID: {blog_id})")
    
    return {
        "message": f"{blog.blog_type} backfill started for blog: {blog.name}",
        "blog_id": blog_id,
        "blog_name": blog.name,
        "blog_type": blog.blog_type,
        "site_url": blog.site_url
    }


def perform_backfill(blog_id: int):
    """백그라운드에서 실행되는 통합 백필링 작업"""
    import asyncio
    
    async def _async_backfill():
        crawler = None
        
        try:
            # 새로운 세션 생성
            session = SessionLocal()
            
            try:
                # 블로그 정보 재조회
                blog = session.query(BlogEntity).filter(BlogEntity.id == blog_id).first()
                
                if not blog:
                    logger.error(f"Blog with ID {blog_id} not found during backfill")
                    return
                
                # 크롤러 팩토리에서 적절한 크롤러 생성
                crawler = CrawlerFactory.create_crawler(blog)
                
                logger.info(f"Starting {blog.blog_type} backfill for blog: {blog.name}")
                
                # 백필링 및 저장 실행
                result = await crawler.backfill_and_save_to_db(blog)
                
                logger.info(f"{blog.blog_type} backfill completed for blog: {blog.name}. "
                           f"Crawled: {result['total']}, Saved: {result['saved']}, "
                           f"Skipped: {result['skipped']}, Errors: {result['errors']}")
                
            finally:
                session.close()
                
        except Exception as e:
            logger.error(f"Error during {blog.blog_type if 'blog' in locals() else 'unknown'} backfill for blog ID {blog_id}: {e}")
        finally:
            if crawler:
                await crawler.close()
    
    # 새로운 이벤트 루프에서 실행
    try:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        loop.run_until_complete(_async_backfill())
    finally:
        loop.close()


@router.get("/status")
def get_backfill_status() -> Dict[str, Any]:
    """백필링 상태 확인 (추후 구현)"""
    return {
        "message": "Backfill status endpoint - TODO: implement actual status tracking",
        "active_tasks": 0,  # 실제로는 Redis나 메모리에서 활성 작업 수를 추적
        "completed_tasks": 0
    }