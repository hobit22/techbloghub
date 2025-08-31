"""
ElasticSearch 인덱싱 API
"""

from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session
from typing import Dict, Any, List, Optional
from loguru import logger

from ..core.database import get_db
from ..models import PostEntity
from ..services.elasticsearch_indexer import elasticsearch_indexer
from ..services.tag_classifier import tag_classifier

router = APIRouter(prefix="/indexing", tags=["indexing"])


@router.post("/posts")
async def index_all_posts(
    background_tasks: BackgroundTasks,
    limit: Optional[int] = None,
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """
    모든 포스트를 ElasticSearch에 인덱싱
    대량 데이터이므로 백그라운드에서 처리
    """
    try:
        # 인덱스 생성/확인
        if not elasticsearch_indexer.ensure_index_exists():
            raise HTTPException(status_code=500, detail="Failed to create/verify ElasticSearch index")
        
        # 백그라운드에서 인덱싱 실행
        background_tasks.add_task(run_bulk_indexing, db, limit)
        
        # 총 포스트 개수 조회
        total_posts = db.query(PostEntity).count()
        actual_limit = min(limit, total_posts) if limit else total_posts
        
        return {
            "message": "Bulk indexing started",
            "total_posts": total_posts,
            "posts_to_index": actual_limit,
            "status": "started"
        }
        
    except Exception as e:
        logger.error(f"Error starting bulk indexing: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/posts/{post_id}")
async def index_single_post(
    post_id: int,
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """특정 포스트 하나를 ElasticSearch에 인덱싱"""
    try:
        # 포스트 조회
        post = db.query(PostEntity).filter(PostEntity.id == post_id).first()
        if not post:
            raise HTTPException(status_code=404, detail=f"Post not found: {post_id}")
        
        # 인덱스 생성/확인
        if not elasticsearch_indexer.ensure_index_exists():
            raise HTTPException(status_code=500, detail="Failed to create/verify ElasticSearch index")
        
        # 태그 추출
        namespace_tags = {}
        if post.title or post.content:
            title = post.title or ""
            content = post.content or ""
            namespace_tags = tag_classifier.extract_tags_from_text(content, title)
        
        # 인덱싱
        success = elasticsearch_indexer.index_post(post, namespace_tags)
        
        if success:
            return {
                "message": "Post indexed successfully",
                "post_id": post_id,
                "title": post.title,
                "tags_extracted": sum(len(tags) for tags in namespace_tags.values()) if namespace_tags else 0
            }
        else:
            raise HTTPException(status_code=500, detail="Failed to index post")
            
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error indexing post {post_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/status")
async def get_indexing_status() -> Dict[str, Any]:
    """ElasticSearch 인덱싱 상태 조회"""
    try:
        # ElasticSearch 연결 상태
        es_available = elasticsearch_indexer.es_client is not None
        
        # 인덱스 통계
        stats = None
        if es_available:
            stats = elasticsearch_indexer.get_index_stats()
        
        return {
            "elasticsearch_available": es_available,
            "index_name": elasticsearch_indexer.index_name,
            "index_stats": stats
        }
        
    except Exception as e:
        logger.error(f"Error getting indexing status: {e}")
        return {
            "elasticsearch_available": False,
            "error": str(e)
        }


@router.delete("/posts/{post_id}")
async def delete_post_from_index(
    post_id: int
) -> Dict[str, Any]:
    """ElasticSearch 인덱스에서 특정 포스트 삭제"""
    try:
        success = elasticsearch_indexer.delete_post(post_id)
        
        if success:
            return {
                "message": "Post deleted from index successfully",
                "post_id": post_id
            }
        else:
            raise HTTPException(status_code=500, detail="Failed to delete post from index")
            
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error deleting post {post_id} from index: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def run_bulk_indexing(db: Session, limit: Optional[int] = None):
    """백그라운드에서 실행되는 대량 인덱싱 함수"""
    try:
        logger.info("Starting bulk indexing of posts")
        
        # 포스트 조회
        query = db.query(PostEntity).order_by(PostEntity.id)
        if limit:
            query = query.limit(limit)
        
        posts = query.all()
        logger.info(f"Found {len(posts)} posts to index")
        
        # 배치별로 처리 (100개씩)
        batch_size = 100
        total_success = 0
        total_failed = 0
        
        for i in range(0, len(posts), batch_size):
            batch = posts[i:i + batch_size]
            logger.info(f"Processing batch {i//batch_size + 1}: posts {i+1}-{min(i+batch_size, len(posts))}")
            
            # 각 포스트에 대해 태그 추출 및 인덱싱 데이터 준비
            posts_data = []
            for post in batch:
                try:
                    # 태그 추출
                    namespace_tags = {}
                    if post.title or post.content:
                        title = post.title or ""
                        content = post.content or ""
                        namespace_tags = tag_classifier.extract_tags_from_text(content, title)
                    
                    posts_data.append((post, namespace_tags, []))
                    
                except Exception as e:
                    logger.error(f"Error preparing post {post.id} for indexing: {e}")
                    total_failed += 1
            
            # 배치 인덱싱
            if posts_data:
                success_count, failed_count = elasticsearch_indexer.bulk_index_posts(posts_data)
                total_success += success_count
                total_failed += failed_count
        
        logger.info(f"Bulk indexing completed: {total_success} success, {total_failed} failed")
        
    except Exception as e:
        logger.error(f"Error in bulk indexing: {e}")
    finally:
        db.close()