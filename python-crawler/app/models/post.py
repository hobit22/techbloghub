from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey, Index
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
    
    # Indexes
    __table_args__ = (
        Index('idx_post_published_at', 'published_at'),
        Index('idx_post_blog_id', 'blog_id'),
    )