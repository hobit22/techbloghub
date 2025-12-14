"""
Post Service Layer
포스트 관련 비즈니스 로직 처리
"""

import logging
from typing import List, Optional, Dict, Any
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.models import Post, Blog
from app.schemas import PostCreate, PostUpdate
from app.schemas.blog import BlogInfo
from app.schemas.post import PostSearchResponse
from app.repositories import PostRepository, BlogRepository

logger = logging.getLogger(__name__)


class PostService:
    """포스트 비즈니스 로직 처리"""

    def __init__(self, db: AsyncSession):
        self.db = db
        self.repository = PostRepository(db)
        self.blog_repository = BlogRepository(db)

    async def get_post_by_id(self, post_id: int, include_blog: bool = False) -> Optional[Post]:
        """
        ID로 포스트 조회

        Args:
            post_id: 포스트 ID
            include_blog: 블로그 정보 포함 여부

        Returns:
            Post 또는 None
        """
        logger.info(f"Fetching post with id={post_id}, include_blog={include_blog}")

        post = await self.repository.get_by_id(post_id, include_blog=include_blog)

        if post:
            logger.info(f"Post found: id={post_id}, title={post.title}")
        else:
            logger.warning(f"Post not found: id={post_id}")

        return post

    async def get_posts(
        self,
        skip: int = 0,
        limit: int = 20,
        blog_id: Optional[int] = None,
        include_blog: bool = False
    ) -> tuple[List[Post], int]:
        """
        포스트 목록 조회 (페이지네이션)

        Args:
            skip: 건너뛸 개수
            limit: 최대 조회 개수
            blog_id: 특정 블로그 필터링
            include_blog: 블로그 정보 포함 여부

        Returns:
            (포스트 리스트, 전체 개수)
        """
        logger.info(f"Fetching posts: skip={skip}, limit={limit}, blog_id={blog_id}")

        posts, total = await self.repository.get_all(
            skip=skip,
            limit=limit,
            blog_id=blog_id,
            include_blog=include_blog
        )

        logger.info(f"Fetched {len(posts)} posts out of {total} total")
        return posts, total

    async def search_posts(
        self,
        query: str,
        limit: int = 20,
        offset: int = 0
    ) -> tuple[List[Dict[str, Any]], int]:
        """
        포스트 전문 검색 (PostgreSQL Full-Text Search)

        Args:
            query: 검색어
            limit: 최대 결과 개수
            offset: 건너뛸 개수

        Returns:
            (검색 결과 리스트, 전체 개수)
        """
        logger.info(f"Searching posts: query='{query}', limit={limit}, offset={offset}")

        # Repository를 통해 검색
        raw_results, total = await self.repository.search_fulltext(
            search_query=query,
            limit=limit,
            offset=offset
        )

        # 결과 변환 (BlogInfo 추가)
        search_results = []
        for row in raw_results:
            blog_info = BlogInfo(
                id=row["blog_id"],
                name=row["blog_name"],
                company=row["blog_company"],
                site_url=row["blog_site_url"],
                logo_url=row["blog_logo_url"]
            )

            post_dict = {
                "id": row["id"],
                "title": row["title"],
                "content": row["content"],
                "author": row["author"],
                "original_url": row["original_url"],
                "normalized_url": row["normalized_url"],
                "blog_id": row["blog_id"],
                "published_at": row["published_at"],
                "created_at": row["created_at"],
                "updated_at": row["updated_at"],
                "keywords": [],
                "blog": blog_info,
                "rank": row["rank"]
            }
            search_results.append(post_dict)

        logger.info(f"Found {len(search_results)} posts out of {total} total for query '{query}'")
        return search_results, total

    async def check_duplicate_url(
        self,
        original_url: str,
        normalized_url: str,
        exclude_id: Optional[int] = None
    ) -> bool:
        """
        포스트 URL 중복 체크

        Args:
            original_url: 원본 URL
            normalized_url: 정규화된 URL
            exclude_id: 제외할 포스트 ID

        Returns:
            중복이면 True, 아니면 False
        """
        logger.info(f"Checking duplicate URL: original={original_url}, normalized={normalized_url}")

        is_duplicate = await self.repository.exists_by_url(
            original_url=original_url,
            normalized_url=normalized_url,
            exclude_id=exclude_id
        )

        if is_duplicate:
            logger.warning(f"Duplicate post URL found")
        else:
            logger.info("No duplicate URL found")

        return is_duplicate

    async def create_post(self, post_data: PostCreate) -> Post:
        """
        새로운 포스트 생성

        Args:
            post_data: 포스트 생성 데이터

        Returns:
            생성된 Post

        Raises:
            ValueError: 블로그가 없거나 중복된 URL이 존재하는 경우
        """
        logger.info(f"Creating post: title={post_data.title}, blog_id={post_data.blog_id}")

        # 블로그 존재 여부 확인
        blog = await self.blog_repository.get_by_id(post_data.blog_id)
        if not blog:
            logger.error(f"Failed to create post: blog_id={post_data.blog_id} not found")
            raise ValueError(f"Blog with id {post_data.blog_id} not found")

        # URL 정규화
        normalized_url = Post.normalize_url(post_data.original_url)

        # 중복 체크
        if await self.check_duplicate_url(post_data.original_url, normalized_url):
            logger.error(f"Failed to create post: duplicate URL")
            raise ValueError("Post with this URL already exists")

        # 포스트 생성
        post_dict = post_data.model_dump()
        new_post = Post(**post_dict, normalized_url=normalized_url)
        created_post = await self.repository.create(new_post)

        logger.info(f"Post created successfully: id={created_post.id}, title={created_post.title}")
        return created_post

    async def update_post(
        self,
        post_id: int,
        post_data: PostUpdate
    ) -> Post:
        """
        포스트 수정

        Args:
            post_id: 포스트 ID
            post_data: 수정할 데이터

        Returns:
            수정된 Post

        Raises:
            ValueError: 포스트가 없는 경우
        """
        logger.info(f"Updating post: id={post_id}")

        # 포스트 조회
        post = await self.get_post_by_id(post_id)
        if not post:
            logger.error(f"Failed to update post: id={post_id} not found")
            raise ValueError(f"Post with id {post_id} not found")

        # 업데이트
        update_data = post_data.model_dump(exclude_unset=True)
        for key, value in update_data.items():
            setattr(post, key, value)

        updated_post = await self.repository.update(post)

        logger.info(f"Post updated successfully: id={post_id}")
        return updated_post

    async def delete_post(self, post_id: int) -> None:
        """
        포스트 삭제

        Args:
            post_id: 포스트 ID

        Raises:
            ValueError: 포스트가 없는 경우
        """
        logger.info(f"Deleting post: id={post_id}")

        post = await self.get_post_by_id(post_id)
        if not post:
            logger.error(f"Failed to delete post: id={post_id} not found")
            raise ValueError(f"Post with id {post_id} not found")

        await self.repository.delete(post)

        logger.info(f"Post deleted successfully: id={post_id}")
