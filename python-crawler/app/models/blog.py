from sqlalchemy import Column, Integer, String, DateTime, Enum as SQLEnum, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from enum import Enum
from ..core.database import Base


class BlogStatus(str, Enum):
    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE" 
    SUSPENDED = "SUSPENDED"


class BlogType(str, Enum):
    WORDPRESS = "WORDPRESS"
    NAVER_D2 = "NAVER_D2" 
    NHN_CLOUD = "NHN_CLOUD"
    LYCORP = "LYCORP"
    MEDIUM = "MEDIUM"
    KAKAO = "KAKAO"
    TOSS = "TOSS"


class BlogEntity(Base):
    __tablename__ = "blog"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False, unique=True, index=True)
    company = Column(String, nullable=False)
    rss_url = Column(String, nullable=False, unique=True)
    site_url = Column(String, nullable=False)
    description = Column(Text)
    logo_url = Column(String)
    status = Column(SQLEnum(BlogStatus), default=BlogStatus.ACTIVE)
    blog_type = Column(SQLEnum(BlogType), default=None)
    last_crawled_at = Column(DateTime)
    
    # Timestamps
    created_at = Column(DateTime(timezone=True), default=func.now(), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), default=func.now(), onupdate=func.now())
    
    # Relationships
    posts = relationship("PostEntity", back_populates="blog", cascade="all, delete-orphan")