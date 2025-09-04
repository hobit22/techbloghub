"""Tag management API endpoints."""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import Dict, Any
from loguru import logger

from ..core.database import get_db
from ..services.tag_service import TagService
from ..models import PostEntity, PostTagEntity

router = APIRouter(prefix="/tags", tags=["tags"])


@router.post("/posts/{post_id}/auto-tag")
async def auto_tag_post(
    post_id: int,
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Auto-tag a post using LLM."""
    try:
        # Find the post
        post = db.query(PostEntity).filter(PostEntity.id == post_id).first()
        if not post:
            raise HTTPException(status_code=404, detail="Post not found")
        
        tag_service = TagService(db)
        
        # Auto-tag with LLM
        tags_assigned, categories_assigned = tag_service.tag_post_with_llm(post)
        
        # Refresh to get updated relationships
        db.refresh(post)
        
        return {
            "message": f"Successfully auto-tagged post {post_id}",
            "post_id": post_id,
            "tags_assigned": tags_assigned,
            "categories_assigned": categories_assigned,
            "tags": [tag.name for tag in post.tags],
            "categories": [category.name for category in post.categories]
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error auto-tagging post {post_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/untagged/auto-tag")
async def auto_tag_untagged_posts(
    limit: int = 10,
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """태그가 없는 포스트들을 지정된 개수만큼 자동 태깅"""
    try:
        # Find posts that have no tags (limit to specified count)
        untagged_posts = db.query(PostEntity).filter(
            ~PostEntity.id.in_(
                db.query(PostTagEntity.post_id).distinct()
            )
        ).limit(limit).all()
        
        if not untagged_posts:
            return {
                "message": "No untagged posts found",
                "posts_processed": 0,
                "total_tags_assigned": 0,
                "total_categories_assigned": 0
            }
        
        tag_service = TagService(db)
        total_tags_assigned = 0
        total_categories_assigned = 0
        posts_processed = 0
        
        for post in untagged_posts:
            try:
                tags_assigned, categories_assigned = tag_service.tag_post_with_llm(post)
                total_tags_assigned += tags_assigned
                total_categories_assigned += categories_assigned
                posts_processed += 1
                
                logger.info(f"Tagged post {post.id}: {tags_assigned} tags, {categories_assigned} categories")
                
            except Exception as e:
                logger.error(f"Failed to tag post {post.id}: {e}")
                continue
        
        return {
            "message": f"Successfully processed {posts_processed} untagged posts (limit: {limit})",
            "limit": limit,
            "posts_found": len(untagged_posts),
            "posts_processed": posts_processed,
            "total_tags_assigned": total_tags_assigned,
            "total_categories_assigned": total_categories_assigned
        }
        
    except Exception as e:
        logger.error(f"Error auto-tagging untagged posts: {e}")
        raise HTTPException(status_code=500, detail=str(e))