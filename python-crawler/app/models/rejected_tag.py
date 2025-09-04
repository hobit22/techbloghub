from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Index, CheckConstraint
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from enum import Enum
from ..core.database import Base


class RejectedStatus(str, Enum):
    PENDING = "PENDING"
    APPROVED = "APPROVED" 
    IGNORED = "IGNORED"


class RejectedTagEntity(Base):
    __tablename__ = "rejected_tags"
    
    id = Column(Integer, primary_key=True, index=True)
    tag_name = Column(String(100), nullable=False)
    post_id = Column(Integer, ForeignKey("posts.id", ondelete="CASCADE"))
    post_title = Column(String(500))
    post_url = Column(String(1023))
    blog_name = Column(String(255))
    rejected_at = Column(DateTime(timezone=True), default=func.now(), server_default=func.now())
    frequency_count = Column(Integer, default=1)
    status = Column(String(20), default=RejectedStatus.PENDING.value)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), default=func.now(), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), default=func.now(), onupdate=func.now())
    
    # Relationships
    post = relationship("PostEntity", backref="rejected_tags")
    
    # Indexes and constraints
    __table_args__ = (
        Index('idx_rejected_tags_name', 'tag_name'),
        Index('idx_rejected_tags_frequency', 'frequency_count', postgresql_using='btree', postgresql_ops={'frequency_count': 'desc'}),
        Index('idx_rejected_tags_status', 'status'),
        Index('idx_rejected_tags_rejected_at', 'rejected_at', postgresql_using='btree', postgresql_ops={'rejected_at': 'desc'}),
        Index('idx_rejected_tags_post_id', 'post_id'),
        CheckConstraint("status IN ('PENDING', 'APPROVED', 'IGNORED')", name='chk_rejected_tag_status'),
    )
    
    def __repr__(self):
        return f"<RejectedTagEntity(id={self.id}, tag_name='{self.tag_name}', status='{self.status}', frequency={self.frequency_count})>"
    
    def to_dict(self):
        """Convert rejected tag entity to dictionary"""
        return {
            "id": self.id,
            "tag_name": self.tag_name,
            "post_id": self.post_id,
            "post_title": self.post_title,
            "post_url": self.post_url,
            "blog_name": self.blog_name,
            "rejected_at": self.rejected_at.isoformat() if self.rejected_at else None,
            "frequency_count": self.frequency_count,
            "status": self.status,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None
        }


class RejectedCategoryEntity(Base):
    __tablename__ = "rejected_categories"
    
    id = Column(Integer, primary_key=True, index=True)
    category_name = Column(String(100), nullable=False)
    post_id = Column(Integer, ForeignKey("posts.id", ondelete="CASCADE"))
    post_title = Column(String(500))
    post_url = Column(String(1023))
    blog_name = Column(String(255))
    rejected_at = Column(DateTime(timezone=True), default=func.now(), server_default=func.now())
    frequency_count = Column(Integer, default=1)
    status = Column(String(20), default=RejectedStatus.PENDING.value)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), default=func.now(), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), default=func.now(), onupdate=func.now())
    
    # Relationships
    post = relationship("PostEntity", backref="rejected_categories")
    
    # Indexes and constraints
    __table_args__ = (
        Index('idx_rejected_categories_name', 'category_name'),
        Index('idx_rejected_categories_frequency', 'frequency_count', postgresql_using='btree', postgresql_ops={'frequency_count': 'desc'}),
        Index('idx_rejected_categories_status', 'status'),
        Index('idx_rejected_categories_rejected_at', 'rejected_at', postgresql_using='btree', postgresql_ops={'rejected_at': 'desc'}),
        CheckConstraint("status IN ('PENDING', 'APPROVED', 'IGNORED')", name='chk_rejected_category_status'),
    )
    
    def __repr__(self):
        return f"<RejectedCategoryEntity(id={self.id}, category_name='{self.category_name}', status='{self.status}', frequency={self.frequency_count})>"
    
    def to_dict(self):
        """Convert rejected category entity to dictionary"""
        return {
            "id": self.id,
            "category_name": self.category_name,
            "post_id": self.post_id,
            "post_title": self.post_title,
            "post_url": self.post_url,
            "blog_name": self.blog_name,
            "rejected_at": self.rejected_at.isoformat() if self.rejected_at else None,
            "frequency_count": self.frequency_count,
            "status": self.status,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None
        }