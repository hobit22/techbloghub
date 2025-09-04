from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey, Index
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from ..core.database import Base


class TagEntity(Base):
    __tablename__ = "tags"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False, unique=True)
    slug = Column(String(100), nullable=False, unique=True)
    description = Column(String(500))
    tag_group = Column(String(50))
    color = Column(String(7))  # Hex color code
    usage_count = Column(Integer, default=0)
    is_active = Column(Boolean, default=True)
    parent_id = Column(Integer, ForeignKey("tags.id"))
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), default=func.now(), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), default=func.now(), onupdate=func.now())
    
    # Self-referential relationship for parent/child tags
    parent = relationship("TagEntity", remote_side=[id], back_populates="children")
    children = relationship("TagEntity", back_populates="parent")
    
    # Many-to-many relationship with posts
    posts = relationship("PostEntity", secondary="post_tags", back_populates="tags")
    
    # Indexes
    __table_args__ = (
        Index('idx_tag_name', 'name'),
        Index('idx_tag_slug', 'slug'),
        Index('idx_tag_group', 'tag_group'),
        Index('idx_tag_usage_count', 'usage_count'),
        Index('idx_tag_active', 'is_active'),
        Index('idx_tag_parent_id', 'parent_id'),
    )
    
    def __repr__(self):
        return f"<TagEntity(id={self.id}, name='{self.name}', group='{self.tag_group}')>"
    
    def to_dict(self):
        """Convert tag entity to dictionary"""
        return {
            "id": self.id,
            "name": self.name,
            "slug": self.slug,
            "description": self.description,
            "tag_group": self.tag_group,
            "color": self.color,
            "usage_count": self.usage_count,
            "is_active": self.is_active,
            "parent_id": self.parent_id,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None
        }


class PostTagEntity(Base):
    __tablename__ = "post_tags"
    
    id = Column(Integer, primary_key=True)
    post_id = Column(Integer, ForeignKey("posts.id"), nullable=False)
    tag_id = Column(Integer, ForeignKey("tags.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), default=func.now(), server_default=func.now())
    
    # Indexes
    __table_args__ = (
        Index('idx_post_tags_post_id', 'post_id'),
        Index('idx_post_tags_tag_id', 'tag_id'),
        Index('idx_post_tags_composite', 'tag_id', 'post_id'),
    )
    
    def __repr__(self):
        return f"<PostTagEntity(post_id={self.post_id}, tag_id={self.tag_id})>"


class CategoryEntity(Base):
    __tablename__ = "categories"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False, unique=True)
    description = Column(String(500))
    color = Column(String(7))  # Hex color code
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), default=func.now(), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), default=func.now(), onupdate=func.now())
    
    # Many-to-many relationship with posts
    posts = relationship("PostEntity", secondary="post_categories", back_populates="categories")
    
    # Indexes
    __table_args__ = (
        Index('idx_category_name', 'name'),
    )
    
    def __repr__(self):
        return f"<CategoryEntity(id={self.id}, name='{self.name}')>"
    
    def to_dict(self):
        """Convert category entity to dictionary"""
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "color": self.color,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None
        }


class PostCategoryEntity(Base):
    __tablename__ = "post_categories"
    
    id = Column(Integer, primary_key=True)
    post_id = Column(Integer, ForeignKey("posts.id"), nullable=False)
    category_id = Column(Integer, ForeignKey("categories.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), default=func.now(), server_default=func.now())
    
    # Indexes
    __table_args__ = (
        Index('idx_post_categories_post_id', 'post_id'),
        Index('idx_post_categories_category_id', 'category_id'),
        Index('idx_post_categories_composite', 'category_id', 'post_id'),
    )
    
    def __repr__(self):
        return f"<PostCategoryEntity(post_id={self.post_id}, category_id={self.category_id})>"