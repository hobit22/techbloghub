"""
Repository Layer
데이터베이스 접근 로직을 캡슐화
"""

from .blog_repository import BlogRepository
from .post_repository import PostRepository

__all__ = ["BlogRepository", "PostRepository"]
