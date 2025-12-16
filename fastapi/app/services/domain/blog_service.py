"""
Blog Service Layer
블로그 관련 비즈니스 로직 처리
"""

import logging
from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import Blog
from app.models.blog import BlogStatus
from app.schemas import BlogCreate, BlogUpdate
from app.repositories import BlogRepository

logger = logging.getLogger(__name__)


class BlogService:
    """블로그 비즈니스 로직 처리"""

    def __init__(self, db: AsyncSession):
        self.db = db
        self.repository = BlogRepository(db)

    async def get_blog_by_id(self, blog_id: int) -> Optional[Blog]:
        """
        ID로 블로그 조회

        Args:
            blog_id: 블로그 ID

        Returns:
            Blog 또는 None
        """
        logger.info(f"Fetching blog with id={blog_id}")
        blog = await self.repository.get_by_id(blog_id)

        if blog:
            logger.info(f"Blog found: id={blog_id}, name={blog.name}")
        else:
            logger.warning(f"Blog not found: id={blog_id}")

        return blog

    async def get_blogs(
        self,
        skip: int = 0,
        limit: int = 20
    ) -> tuple[List[Blog], int]:
        """
        블로그 목록 조회 (페이지네이션)

        Args:
            skip: 건너뛸 개수
            limit: 최대 조회 개수

        Returns:
            (블로그 리스트, 전체 개수)
        """
        logger.info(f"Fetching blogs: skip={skip}, limit={limit}")

        blogs, total = await self.repository.get_all(skip=skip, limit=limit)

        logger.info(f"Fetched {len(blogs)} blogs out of {total} total")
        return blogs, total

    async def get_active_blogs(self) -> List[Blog]:
        """
        활성 상태의 블로그 목록 조회 (포스트 통계 포함)

        Returns:
            활성 블로그 리스트 (post_count, latest_post_published_at 포함)
        """
        logger.info("Fetching active blogs")

        blogs = await self.repository.get_active()

        # 블로그별 포스트 통계 조회
        blog_ids = [blog.id for blog in blogs]
        post_stats = await self.repository.get_post_stats(blog_ids)

        # 각 블로그 객체에 통계 정보 추가
        for blog in blogs:
            stats = post_stats.get(blog.id, {"post_count": 0, "latest_post_published_at": None})
            blog.post_count = stats["post_count"]
            blog.latest_post_published_at = stats["latest_post_published_at"]

        logger.info(f"Fetched {len(blogs)} active blogs with post stats")
        return blogs

    async def check_duplicate(
        self,
        name: Optional[str] = None,
        rss_url: Optional[str] = None,
        exclude_id: Optional[int] = None
    ) -> bool:
        """
        블로그 중복 체크 (name 또는 rss_url)

        Args:
            name: 블로그 이름
            rss_url: RSS URL
            exclude_id: 제외할 블로그 ID (수정 시 사용)

        Returns:
            중복이면 True, 아니면 False
        """
        logger.info(f"Checking duplicate: name={name}, rss_url={rss_url}, exclude_id={exclude_id}")

        is_duplicate = await self.repository.exists_duplicate(
            name=name,
            rss_url=rss_url,
            exclude_id=exclude_id
        )

        if is_duplicate:
            logger.warning(f"Duplicate blog found")
        else:
            logger.info("No duplicate found")

        return is_duplicate

    async def create_blog(self, blog_data: BlogCreate) -> Blog:
        """
        새로운 블로그 생성

        Args:
            blog_data: 블로그 생성 데이터

        Returns:
            생성된 Blog

        Raises:
            ValueError: 중복된 name 또는 rss_url이 존재하는 경우
        """
        logger.info(f"Creating blog: name={blog_data.name}, rss_url={blog_data.rss_url}")

        # 중복 체크
        if await self.check_duplicate(name=blog_data.name, rss_url=blog_data.rss_url):
            logger.error(f"Failed to create blog: duplicate name or rss_url")
            raise ValueError("Blog with this name or RSS URL already exists")

        # 블로그 생성
        new_blog = Blog(**blog_data.model_dump())
        created_blog = await self.repository.create(new_blog)

        logger.info(f"Blog created successfully: id={created_blog.id}, name={created_blog.name}")
        return created_blog

    async def update_blog(
        self,
        blog_id: int,
        blog_data: BlogUpdate
    ) -> Blog:
        """
        블로그 수정

        Args:
            blog_id: 블로그 ID
            blog_data: 수정할 데이터

        Returns:
            수정된 Blog

        Raises:
            ValueError: 블로그가 없거나 중복된 데이터가 있는 경우
        """
        logger.info(f"Updating blog: id={blog_id}")

        # 블로그 조회
        blog = await self.get_blog_by_id(blog_id)
        if not blog:
            logger.error(f"Failed to update blog: id={blog_id} not found")
            raise ValueError(f"Blog with id {blog_id} not found")

        # 업데이트할 데이터 추출
        update_data = blog_data.model_dump(exclude_unset=True)

        # 중복 체크 (name, rss_url 변경 시)
        if "name" in update_data or "rss_url" in update_data:
            if await self.check_duplicate(
                name=update_data.get("name", blog.name),
                rss_url=update_data.get("rss_url", blog.rss_url),
                exclude_id=blog_id
            ):
                logger.error(f"Failed to update blog: duplicate name or rss_url")
                raise ValueError("Blog with this name or RSS URL already exists")

        # 업데이트
        for key, value in update_data.items():
            setattr(blog, key, value)

        updated_blog = await self.repository.update(blog)

        logger.info(f"Blog updated successfully: id={blog_id}")
        return updated_blog

    async def delete_blog(self, blog_id: int) -> None:
        """
        블로그 삭제

        Args:
            blog_id: 블로그 ID

        Raises:
            ValueError: 블로그가 없는 경우
        """
        logger.info(f"Deleting blog: id={blog_id}")

        blog = await self.get_blog_by_id(blog_id)
        if not blog:
            logger.error(f"Failed to delete blog: id={blog_id} not found")
            raise ValueError(f"Blog with id {blog_id} not found")

        await self.repository.delete(blog)

        logger.info(f"Blog deleted successfully: id={blog_id}")
