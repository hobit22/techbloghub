from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey, Index, Boolean, Float
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from ..core.database import Base


class PostEntity(Base):
    __tablename__ = "posts"
    
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    content = Column(Text)
    original_url = Column(String(1023), nullable=False, unique=True)
    author = Column(String)
    published_at = Column(DateTime)
    
    
    # Foreign keys
    blog_id = Column(Integer, ForeignKey("blog.id"), nullable=False)
    
    # Timestamps  
    created_at = Column(DateTime(timezone=True), default=func.now(), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), default=func.now(), onupdate=func.now())
    
    # Relationships
    blog = relationship("BlogEntity", back_populates="posts")
    
    # Many-to-many relationships with tags and categories
    tags = relationship("TagEntity", secondary="post_tags", back_populates="posts")
    categories = relationship("CategoryEntity", secondary="post_categories", back_populates="posts")
    
    # Indexes
    __table_args__ = (
        Index('idx_post_published_at', 'published_at'),
        Index('idx_post_blog_id', 'blog_id'),
        Index('idx_post_original_url', 'original_url'),
    )
    
    def to_dict(self, include_tags=True, include_categories=True):
        """Convert post entity to dictionary"""
        result = {
            "id": self.id,
            "title": self.title,
            "content": self.content,
            "original_url": self.original_url,
            "author": self.author,
            "published_at": self.published_at.isoformat() if self.published_at else None,
            "blog_id": self.blog_id,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None
        }
        
        if include_tags and self.tags:
            result["tags"] = [tag.to_dict() for tag in self.tags]
        
        if include_categories and self.categories:
            result["categories"] = [category.to_dict() for category in self.categories]
        
        if self.blog:
            result["blog"] = {
                "id": self.blog.id,
                "name": self.blog.name,
                "company": self.blog.company
            }
        
        return result