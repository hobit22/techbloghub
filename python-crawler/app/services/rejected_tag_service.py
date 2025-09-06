"""Service for managing rejected tags and categories."""

from typing import List, Dict, Optional, Any
from sqlalchemy.orm import Session
from sqlalchemy import func, desc, and_, or_
from loguru import logger

from ..models import PostEntity, RejectedTagEntity, RejectedCategoryEntity, RejectedStatus


class RejectedTagService:
    """Service for tracking and managing rejected tags and categories."""
    
    def __init__(self, db: Session):
        self.db = db
    
    def save_rejected_tag(self, tag_name: str, post: PostEntity) -> Optional[RejectedTagEntity]:
        """Save a rejected tag with post context."""
        try:
            # Check if this tag was already rejected for this post
            existing = self.db.query(RejectedTagEntity).filter(
                and_(
                    RejectedTagEntity.tag_name == tag_name,
                    RejectedTagEntity.post_id == post.id
                )
            ).first()
            
            if existing:
                # Increment frequency count
                existing.frequency_count += 1
                existing.updated_at = func.now()
                logger.debug(f"Incremented frequency for rejected tag '{tag_name}' to {existing.frequency_count}")
            else:
                # Create new rejected tag entry
                rejected_tag = RejectedTagEntity(
                    tag_name=tag_name,
                    post_id=post.id,
                    post_title=post.title,
                    post_url=post.original_url,
                    blog_name=post.blog.name if post.blog else None,
                    frequency_count=1,
                    status=RejectedStatus.PENDING.value
                )
                
                self.db.add(rejected_tag)
                existing = rejected_tag
                logger.debug(f"Saved new rejected tag '{tag_name}' for post {post.id}")
            
            self.db.commit()
            return existing
            
        except Exception as e:
            logger.error(f"Error saving rejected tag '{tag_name}': {e}")
            self.db.rollback()
            return None
    
    def save_rejected_category(self, category_name: str, post: PostEntity) -> Optional[RejectedCategoryEntity]:
        """Save a rejected category with post context."""
        try:
            # Check if this category was already rejected for this post
            existing = self.db.query(RejectedCategoryEntity).filter(
                and_(
                    RejectedCategoryEntity.category_name == category_name,
                    RejectedCategoryEntity.post_id == post.id
                )
            ).first()
            
            if existing:
                # Increment frequency count
                existing.frequency_count += 1
                existing.updated_at = func.now()
                logger.debug(f"Incremented frequency for rejected category '{category_name}' to {existing.frequency_count}")
            else:
                # Create new rejected category entry
                rejected_category = RejectedCategoryEntity(
                    category_name=category_name,
                    post_id=post.id,
                    post_title=post.title,
                    post_url=post.original_url,
                    blog_name=post.blog.name if post.blog else None,
                    frequency_count=1,
                    status=RejectedStatus.PENDING.value
                )
                
                self.db.add(rejected_category)
                existing = rejected_category
                logger.debug(f"Saved new rejected category '{category_name}' for post {post.id}")
            
            self.db.commit()
            return existing
            
        except Exception as e:
            logger.error(f"Error saving rejected category '{category_name}': {e}")
            self.db.rollback()
            return None
    
    def get_rejected_tags(self, status: Optional[str] = None, limit: int = 100) -> List[RejectedTagEntity]:
        """Get rejected tags, optionally filtered by status."""
        query = self.db.query(RejectedTagEntity)
        
        if status:
            query = query.filter(RejectedTagEntity.status == status)
        
        return query.order_by(desc(RejectedTagEntity.frequency_count)).limit(limit).all()
    
    def get_rejected_tag_stats(self, min_frequency: int = 1) -> List[Dict[str, Any]]:
        """Get rejected tag statistics grouped by tag name."""
        results = self.db.query(
            RejectedTagEntity.tag_name,
            func.count(RejectedTagEntity.id).label('occurrence_count'),
            func.sum(RejectedTagEntity.frequency_count).label('total_frequency'),
            func.min(RejectedTagEntity.rejected_at).label('first_seen'),
            func.max(RejectedTagEntity.rejected_at).label('last_seen'),
            RejectedTagEntity.status,
            func.count(RejectedTagEntity.post_id.distinct()).label('unique_posts')
        ).filter(
            RejectedTagEntity.frequency_count >= min_frequency
        ).group_by(
            RejectedTagEntity.tag_name, RejectedTagEntity.status
        ).order_by(
            desc(func.sum(RejectedTagEntity.frequency_count))
        ).all()
        
        return [
            {
                "tag_name": result.tag_name,
                "occurrence_count": result.occurrence_count,
                "total_frequency": result.total_frequency,
                "first_seen": result.first_seen.isoformat() if result.first_seen else None,
                "last_seen": result.last_seen.isoformat() if result.last_seen else None,
                "status": result.status,
                "unique_posts": result.unique_posts
            }
            for result in results
        ]
    
    def get_approval_candidates(self, min_frequency: int = 5) -> List[Dict[str, Any]]:
        """Get tags that are candidates for approval based on frequency."""
        results = self.db.query(
            RejectedTagEntity.tag_name,
            func.sum(RejectedTagEntity.frequency_count).label('total_frequency'),
            func.count(RejectedTagEntity.post_id.distinct()).label('unique_posts'),
            func.count(RejectedTagEntity.blog_name.distinct()).label('unique_blogs'),
            func.min(RejectedTagEntity.rejected_at).label('first_seen'),
            func.max(RejectedTagEntity.rejected_at).label('last_seen')
        ).filter(
            RejectedTagEntity.status == RejectedStatus.PENDING.value
        ).group_by(
            RejectedTagEntity.tag_name
        ).having(
            func.sum(RejectedTagEntity.frequency_count) >= min_frequency
        ).order_by(
            desc(func.sum(RejectedTagEntity.frequency_count))
        ).all()
        
        return [
            {
                "tag_name": result.tag_name,
                "total_frequency": result.total_frequency,
                "unique_posts": result.unique_posts,
                "unique_blogs": result.unique_blogs,
                "first_seen": result.first_seen.isoformat() if result.first_seen else None,
                "last_seen": result.last_seen.isoformat() if result.last_seen else None
            }
            for result in results
        ]
    
    def get_recent_rejected_tags(self, days: int = 7, min_count: int = 2) -> List[Dict[str, Any]]:
        """Get recently rejected tags that might indicate new trends."""
        from datetime import datetime, timedelta
        
        since_date = datetime.utcnow() - timedelta(days=days)
        
        results = self.db.query(
            RejectedTagEntity.tag_name,
            func.count(RejectedTagEntity.id).label('recent_count'),
            func.sum(RejectedTagEntity.frequency_count).label('total_frequency'),
            func.max(RejectedTagEntity.rejected_at).label('last_rejected')
        ).filter(
            and_(
                RejectedTagEntity.rejected_at >= since_date,
                RejectedTagEntity.status == RejectedStatus.PENDING.value
            )
        ).group_by(
            RejectedTagEntity.tag_name
        ).having(
            func.count(RejectedTagEntity.id) >= min_count
        ).order_by(
            desc(func.sum(RejectedTagEntity.frequency_count))
        ).all()
        
        return [
            {
                "tag_name": result.tag_name,
                "recent_count": result.recent_count,
                "total_frequency": result.total_frequency,
                "last_rejected": result.last_rejected.isoformat() if result.last_rejected else None
            }
            for result in results
        ]
    
    def approve_tag(self, tag_name: str) -> int:
        """Mark a rejected tag as approved."""
        try:
            updated_count = self.db.query(RejectedTagEntity).filter(
                and_(
                    RejectedTagEntity.tag_name == tag_name,
                    RejectedTagEntity.status == RejectedStatus.PENDING.value
                )
            ).update({
                'status': RejectedStatus.APPROVED.value,
                'updated_at': func.now()
            })
            
            self.db.commit()
            logger.info(f"Approved rejected tag '{tag_name}' - {updated_count} records updated")
            return updated_count
            
        except Exception as e:
            logger.error(f"Error approving rejected tag '{tag_name}': {e}")
            self.db.rollback()
            return 0
    
    def ignore_tag(self, tag_name: str) -> int:
        """Mark a rejected tag as ignored."""
        try:
            updated_count = self.db.query(RejectedTagEntity).filter(
                and_(
                    RejectedTagEntity.tag_name == tag_name,
                    RejectedTagEntity.status == RejectedStatus.PENDING.value
                )
            ).update({
                'status': RejectedStatus.IGNORED.value,
                'updated_at': func.now()
            })
            
            self.db.commit()
            logger.info(f"Ignored rejected tag '{tag_name}' - {updated_count} records updated")
            return updated_count
            
        except Exception as e:
            logger.error(f"Error ignoring rejected tag '{tag_name}': {e}")
            self.db.rollback()
            return 0
    
    def batch_approve_tags(self, tag_names: List[str]) -> int:
        """Approve multiple rejected tags at once."""
        try:
            updated_count = self.db.query(RejectedTagEntity).filter(
                and_(
                    RejectedTagEntity.tag_name.in_(tag_names),
                    RejectedTagEntity.status == RejectedStatus.PENDING.value
                )
            ).update({
                'status': RejectedStatus.APPROVED.value,
                'updated_at': func.now()
            })
            
            self.db.commit()
            logger.info(f"Batch approved {len(tag_names)} tags - {updated_count} records updated")
            return updated_count
            
        except Exception as e:
            logger.error(f"Error batch approving tags {tag_names}: {e}")
            self.db.rollback()
            return 0
    
    def get_tag_context(self, tag_name: str, limit: int = 10) -> List[Dict[str, Any]]:
        """Get context information for a rejected tag."""
        results = self.db.query(RejectedTagEntity).filter(
            RejectedTagEntity.tag_name == tag_name
        ).order_by(desc(RejectedTagEntity.rejected_at)).limit(limit).all()
        
        return [
            {
                "post_title": result.post_title,
                "post_url": result.post_url,
                "blog_name": result.blog_name,
                "rejected_at": result.rejected_at.isoformat() if result.rejected_at else None,
                "frequency_count": result.frequency_count
            }
            for result in results
        ]
    
    def get_summary_stats(self) -> Dict[str, Any]:
        """Get overall summary statistics."""
        # Count by status
        status_counts = self.db.query(
            RejectedTagEntity.status,
            func.count(RejectedTagEntity.id).label('count'),
            func.sum(RejectedTagEntity.frequency_count).label('total_frequency')
        ).group_by(RejectedTagEntity.status).all()
        
        # Unique tag names
        unique_tags = self.db.query(
            func.count(func.distinct(RejectedTagEntity.tag_name))
        ).scalar()
        
        # Most frequent tag
        most_frequent = self.db.query(
            RejectedTagEntity.tag_name,
            func.sum(RejectedTagEntity.frequency_count).label('total_frequency')
        ).group_by(RejectedTagEntity.tag_name).order_by(
            desc(func.sum(RejectedTagEntity.frequency_count))
        ).first()
        
        return {
            "total_unique_tags": unique_tags,
            "status_breakdown": {
                status: {"count": count, "total_frequency": freq}
                for status, count, freq in status_counts
            },
            "most_frequent_tag": {
                "name": most_frequent.tag_name if most_frequent else None,
                "frequency": most_frequent.total_frequency if most_frequent else 0
            }
        }