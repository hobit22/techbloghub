"""Tag management service for handling tag operations and relationships."""

from typing import List, Dict, Optional, Tuple, Set
from sqlalchemy.orm import Session
from sqlalchemy import func, and_, desc
from loguru import logger

from ..models import TagEntity, CategoryEntity, PostEntity, PostTagEntity, PostCategoryEntity
from ..services.processing.llm_tagger import LLMTagger, TaggingResult


class TagService:
    """Service for managing tags and categories."""
    
    def __init__(self, db: Session):
        self.db = db
        self.llm_tagger = None  # Initialize lazily when needed
    
    def get_or_create_tag(self, tag_name: str, tag_group: Optional[str] = None, 
                         color: Optional[str] = None, auto_slug: bool = True) -> TagEntity:
        """Get existing tag or create new one."""
        try:
            # Try to find existing tag (case insensitive)
            existing_tag = self.db.query(TagEntity).filter(
                func.lower(TagEntity.name) == tag_name.lower()
            ).first()
            
            if existing_tag:
                return existing_tag
            
            # Generate slug if not provided
            slug = self._generate_slug(tag_name) if auto_slug else tag_name.lower()
            
            # Create new tag
            new_tag = TagEntity(
                name=tag_name,
                slug=slug,
                tag_group=tag_group,
                color=color or '#000000',
                usage_count=0,
                is_active=True
            )
            
            self.db.add(new_tag)
            self.db.commit()
            self.db.refresh(new_tag)
            
            logger.info(f"Created new tag: {tag_name} (group: {tag_group})")
            return new_tag
            
        except Exception as e:
            logger.error(f"Error creating/getting tag '{tag_name}': {e}")
            self.db.rollback()
            raise
    
    def get_or_create_category(self, category_name: str, color: Optional[str] = None) -> CategoryEntity:
        """Get existing category or create new one."""
        try:
            # Try to find existing category
            existing_category = self.db.query(CategoryEntity).filter(
                func.lower(CategoryEntity.name) == category_name.lower()
            ).first()
            
            if existing_category:
                return existing_category
            
            # Create new category
            new_category = CategoryEntity(
                name=category_name,
                color=color or '#000000'
            )
            
            self.db.add(new_category)
            self.db.commit()
            self.db.refresh(new_category)
            
            logger.info(f"Created new category: {category_name}")
            return new_category
            
        except Exception as e:
            logger.error(f"Error creating/getting category '{category_name}': {e}")
            self.db.rollback()
            raise
    
    def assign_tags_to_post(self, post: PostEntity, tag_names: List[str], 
                           replace_existing: bool = True) -> int:
        """Assign tags to a post."""
        try:
            if replace_existing:
                # Remove existing tags
                self.db.query(PostTagEntity).filter(
                    PostTagEntity.post_id == post.id
                ).delete()
            
            assigned_count = 0
            for tag_name in tag_names:
                if not tag_name.strip():
                    continue
                
                tag = self.get_or_create_tag(tag_name.strip())
                
                # Check if relationship already exists
                existing_rel = self.db.query(PostTagEntity).filter(
                    and_(PostTagEntity.post_id == post.id, 
                         PostTagEntity.tag_id == tag.id)
                ).first()
                
                if not existing_rel:
                    post_tag = PostTagEntity(post_id=post.id, tag_id=tag.id)
                    self.db.add(post_tag)
                    
                    # Increment usage count
                    tag.usage_count = (tag.usage_count or 0) + 1
                    assigned_count += 1
            
            self.db.commit()
            logger.debug(f"Assigned {assigned_count} tags to post {post.id}")
            return assigned_count
            
        except Exception as e:
            logger.error(f"Error assigning tags to post {post.id}: {e}")
            self.db.rollback()
            raise
    
    def assign_categories_to_post(self, post: PostEntity, category_names: List[str],
                                replace_existing: bool = True) -> int:
        """Assign categories to a post."""
        try:
            if replace_existing:
                # Remove existing categories
                self.db.query(PostCategoryEntity).filter(
                    PostCategoryEntity.post_id == post.id
                ).delete()
            
            assigned_count = 0
            for category_name in category_names:
                if not category_name.strip():
                    continue
                
                category = self.get_or_create_category(category_name.strip())
                
                # Check if relationship already exists
                existing_rel = self.db.query(PostCategoryEntity).filter(
                    and_(PostCategoryEntity.post_id == post.id,
                         PostCategoryEntity.category_id == category.id)
                ).first()
                
                if not existing_rel:
                    post_category = PostCategoryEntity(post_id=post.id, category_id=category.id)
                    self.db.add(post_category)
                    assigned_count += 1
            
            self.db.commit()
            logger.debug(f"Assigned {assigned_count} categories to post {post.id}")
            return assigned_count
            
        except Exception as e:
            logger.error(f"Error assigning categories to post {post.id}: {e}")
            self.db.rollback()
            raise
    
    def tag_post_with_llm(self, post: PostEntity) -> Tuple[int, int]:
        """Tag a post using LLM and assign tags/categories."""
        try:
            if not self.llm_tagger:
                self.llm_tagger = LLMTagger(db_session=self.db)
            
            # Get tags and categories from LLM
            tagging_result = self.llm_tagger.tag_post(post)
            
            # Assign tags and categories
            tags_assigned = self.assign_tags_to_post(post, tagging_result.tags)
            categories_assigned = self.assign_categories_to_post(post, tagging_result.categories)
            
            logger.info(f"LLM tagged post {post.id}: {tags_assigned} tags, {categories_assigned} categories")
            return tags_assigned, categories_assigned
            
        except Exception as e:
            logger.error(f"Error tagging post {post.id} with LLM: {e}")
            return 0, 0
    
    def get_popular_tags(self, limit: int = 50, tag_group: Optional[str] = None) -> List[TagEntity]:
        """Get most popular tags by usage count."""
        query = self.db.query(TagEntity).filter(TagEntity.is_active == True)
        
        if tag_group:
            query = query.filter(TagEntity.tag_group == tag_group)
        
        return query.order_by(desc(TagEntity.usage_count)).limit(limit).all()
    
    def get_tags_by_group(self, tag_group: str, active_only: bool = True) -> List[TagEntity]:
        """Get all tags in a specific group."""
        query = self.db.query(TagEntity).filter(TagEntity.tag_group == tag_group)
        
        if active_only:
            query = query.filter(TagEntity.is_active == True)
        
        return query.order_by(TagEntity.name).all()
    
    def get_tag_groups(self) -> List[Dict[str, any]]:
        """Get all tag groups with counts."""
        result = self.db.query(
            TagEntity.tag_group,
            func.count(TagEntity.id).label('count'),
            func.sum(TagEntity.usage_count).label('total_usage')
        ).filter(
            TagEntity.is_active == True,
            TagEntity.tag_group.isnot(None)
        ).group_by(TagEntity.tag_group).all()
        
        return [
            {
                "group": row.tag_group,
                "tag_count": row.count,
                "total_usage": row.total_usage or 0
            }
            for row in result
        ]
    
    def search_tags(self, query: str, limit: int = 20) -> List[TagEntity]:
        """Search tags by name."""
        return self.db.query(TagEntity).filter(
            TagEntity.is_active == True,
            TagEntity.name.ilike(f'%{query}%')
        ).order_by(desc(TagEntity.usage_count)).limit(limit).all()
    
    def get_related_posts_by_tags(self, post: PostEntity, limit: int = 10) -> List[PostEntity]:
        """Find related posts based on common tags."""
        if not post.tags:
            return []
        
        # Get tag IDs for the current post
        post_tag_ids = [tag.id for tag in post.tags]
        
        # Find posts that share tags with the current post
        related_posts = self.db.query(PostEntity).join(PostTagEntity).filter(
            PostTagEntity.tag_id.in_(post_tag_ids),
            PostEntity.id != post.id  # Exclude the current post
        ).group_by(PostEntity.id).order_by(
            func.count(PostTagEntity.tag_id).desc(),  # Order by number of common tags
            desc(PostEntity.published_at)  # Then by recency
        ).limit(limit).all()
        
        return related_posts
    
    def update_tag_usage_counts(self) -> Dict[str, int]:
        """Update usage counts for all tags based on actual post relationships."""
        try:
            # Get actual usage counts from post_tags table
            usage_counts = self.db.query(
                PostTagEntity.tag_id,
                func.count(PostTagEntity.post_id).label('usage_count')
            ).group_by(PostTagEntity.tag_id).all()
            
            updated_count = 0
            for tag_id, usage_count in usage_counts:
                self.db.query(TagEntity).filter(TagEntity.id == tag_id).update({
                    'usage_count': usage_count
                })
                updated_count += 1
            
            # Set usage_count to 0 for unused tags
            self.db.query(TagEntity).filter(
                ~TagEntity.id.in_([tag_id for tag_id, _ in usage_counts])
            ).update({'usage_count': 0})
            
            self.db.commit()
            logger.info(f"Updated usage counts for {updated_count} tags")
            
            return {
                "updated_tags": updated_count,
                "total_tags": self.db.query(TagEntity).count()
            }
            
        except Exception as e:
            logger.error(f"Error updating tag usage counts: {e}")
            self.db.rollback()
            raise
    
    def _generate_slug(self, name: str) -> str:
        """Generate URL-friendly slug from tag name."""
        import re
        slug = re.sub(r'[^\w\s-]', '', name.lower())
        slug = re.sub(r'[-\s]+', '-', slug)
        return slug.strip('-')
    
    def get_tagging_statistics(self) -> Dict[str, any]:
        """Get comprehensive tagging statistics."""
        try:
            # Basic counts
            total_tags = self.db.query(TagEntity).filter(TagEntity.is_active == True).count()
            total_categories = self.db.query(CategoryEntity).count()
            total_posts = self.db.query(PostEntity).count()
            
            # Tagged posts
            tagged_posts = self.db.query(PostEntity.id).join(PostTagEntity).distinct().count()
            categorized_posts = self.db.query(PostEntity.id).join(PostCategoryEntity).distinct().count()
            
            # Popular tags
            popular_tags = self.get_popular_tags(10)
            
            return {
                "total_tags": total_tags,
                "total_categories": total_categories,
                "total_posts": total_posts,
                "tagged_posts": tagged_posts,
                "categorized_posts": categorized_posts,
                "tagging_rate": round((tagged_posts / total_posts * 100) if total_posts > 0 else 0, 2),
                "categorization_rate": round((categorized_posts / total_posts * 100) if total_posts > 0 else 0, 2),
                "popular_tags": [{"name": tag.name, "usage_count": tag.usage_count} for tag in popular_tags],
                "tag_groups": self.get_tag_groups()
            }
            
        except Exception as e:
            logger.error(f"Error getting tagging statistics: {e}")
            raise