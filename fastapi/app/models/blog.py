from sqlalchemy import Column, Integer, String, DateTime, Enum as SQLEnum
from sqlalchemy.sql import func
from datetime import datetime
import enum

from app.core.database import Base


class BlogStatus(str, enum.Enum):
    """블로그 상태"""
    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE"
    SUSPENDED = "SUSPENDED"


class BlogType(str, enum.Enum):
    """블로그 타입"""
    COMPANY = "COMPANY"
    PERSONAL = "PERSONAL"


class Blog(Base):
    """
    블로그 모델
    RSS 피드를 통해 기술 블로그를 크롤링하는 대상
    """
    __tablename__ = "blogs"

    # Primary Key
    id = Column(Integer, primary_key=True, index=True)

    # 기본 정보
    name = Column(String(255), nullable=False, unique=True, index=True, comment="블로그 이름")
    company = Column(String(255), nullable=False, comment="회사/개인 이름")
    description = Column(String(500), comment="블로그 설명")

    # URL 정보
    rss_url = Column(String(500), nullable=False, unique=True, index=True, comment="RSS 피드 URL")
    site_url = Column(String(500), nullable=False, comment="블로그 사이트 URL")
    logo_url = Column(String(500), comment="로고 이미지 URL")

    # 상태 관리
    status = Column(
        SQLEnum(BlogStatus, name="blog_status"),
        default=BlogStatus.ACTIVE,
        nullable=False,
        comment="블로그 상태"
    )
    blog_type = Column(
        SQLEnum(BlogType, name="blog_type"),
        default=BlogType.COMPANY,
        comment="블로그 타입"
    )

    # 크롤링 정보
    last_crawled_at = Column(DateTime(timezone=True), comment="마지막 크롤링 시각")
    failure_count = Column(Integer, default=0, comment="크롤링 실패 횟수")

    # 타임스탬프
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    def __repr__(self):
        return f"<Blog(id={self.id}, name='{self.name}', company='{self.company}', status={self.status})>"

    def is_active(self) -> bool:
        """블로그가 활성 상태인지 확인"""
        return self.status == BlogStatus.ACTIVE

    def mark_crawl_success(self):
        """크롤링 성공 시 호출"""
        self.last_crawled_at = datetime.utcnow()
        self.failure_count = 0

    def mark_crawl_failure(self):
        """크롤링 실패 시 호출"""
        self.failure_count += 1
        if self.failure_count >= 5:
            self.status = BlogStatus.SUSPENDED
