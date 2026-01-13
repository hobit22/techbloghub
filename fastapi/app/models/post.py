from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, Index, Enum as SQLEnum
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from sqlalchemy.dialects.postgresql import TSVECTOR
import enum

from app.core.database import Base


class PostStatus(str, enum.Enum):
    """포스트 처리 상태"""
    PENDING = "PENDING"          # URL만 수집됨, 본문 추출 대기
    PROCESSING = "PROCESSING"    # 본문 추출 중
    COMPLETED = "COMPLETED"      # 본문 추출 완료
    FAILED = "FAILED"            # 본문 추출 실패


class Post(Base):
    """
    포스트 모델
    블로그에서 크롤링한 개별 게시글
    """
    __tablename__ = "posts"

    # Primary Key
    id = Column(Integer, primary_key=True, index=True)

    # 기본 정보
    title = Column(String(500), nullable=False, comment="게시글 제목")
    content = Column(Text, comment="게시글 본문 (HTML 또는 Markdown)")
    author = Column(String(200), comment="작성자")

    # URL 정보
    original_url = Column(String(1000), nullable=False, unique=True, index=True, comment="원본 게시글 URL")
    normalized_url = Column(String(1000), unique=True, index=True, comment="정규화된 URL (쿼리 파라미터 제거)")

    # 블로그 관계
    blog_id = Column(Integer, ForeignKey("blogs.id", ondelete="CASCADE"), nullable=False, index=True, comment="블로그 ID")
    blog = relationship("Blog", backref="posts")

    # 발행 정보
    published_at = Column(DateTime(timezone=True), index=True, comment="게시글 발행 시각")

    # 처리 상태
    status = Column(
        SQLEnum(PostStatus, name="post_status"),
        default=PostStatus.PENDING,
        nullable=False,
        index=True,
        comment="본문 추출 상태"
    )
    retry_count = Column(Integer, default=0, comment="본문 추출 재시도 횟수")
    error_message = Column(Text, comment="추출 실패 시 에러 메시지")

    # 키워드 벡터 (PostgreSQL Full-Text Search)
    keyword_vector = Column(TSVECTOR, comment="키워드 검색을 위한 tsvector")

    # 타임스탬프
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    # 인덱스 및 스키마 정의
    __table_args__ = (
        Index('idx_posts_blog_published', 'blog_id', 'published_at'),
        Index('idx_posts_keyword_vector', 'keyword_vector', postgresql_using='gin')
    )

    def __repr__(self):
        return f"<Post(id={self.id}, title='{self.title[:50]}...', blog_id={self.blog_id})>"

    @staticmethod
    def normalize_url(url: str) -> str:
        """
        URL 정규화
        - 쿼리 파라미터 제거
        - Fragment 제거
        - 마지막 슬래시 제거
        """
        if not url:
            return ""

        normalized = url.strip()

        # 쿼리 파라미터 제거
        if '?' in normalized:
            normalized = normalized.split('?')[0]

        # Fragment 제거
        if '#' in normalized:
            normalized = normalized.split('#')[0]

        # 마지막 슬래시 제거
        if normalized.endswith('/'):
            normalized = normalized[:-1]

        return normalized.lower()
