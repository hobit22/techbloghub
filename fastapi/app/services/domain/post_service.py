"""
Post Service Layer
포스트 관련 비즈니스 로직 및 데이터베이스 조작
"""

import logging
from typing import List, Optional, Dict, Any
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, text
from sqlalchemy.orm import selectinload

from app.models import Post, Blog
from app.schemas import PostCreate, PostUpdate
from app.schemas.blog import BlogInfo
from app.schemas.post import PostSearchResponse

logger = logging.getLogger(__name__)


class PostService:
    """포스트 CRUD 및 비즈니스 로직 처리"""

    def __init__(self, db: AsyncSession):
        self.db = db

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

        query = select(Post).where(Post.id == post_id)

        if include_blog:
            query = query.options(selectinload(Post.blog))

        result = await self.db.execute(query)
        post = result.scalar_one_or_none()

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

        # 쿼리 빌드
        query = select(Post)
        count_query = select(func.count(Post.id))

        if include_blog:
            query = query.options(selectinload(Post.blog))

        if blog_id:
            query = query.where(Post.blog_id == blog_id)
            count_query = count_query.where(Post.blog_id == blog_id)

        # 총 개수
        total_result = await self.db.execute(count_query)
        total = total_result.scalar()

        # 포스트 목록
        result = await self.db.execute(
            query
            .order_by(Post.published_at.desc().nulls_last(), Post.created_at.desc())
            .offset(skip)
            .limit(limit)
        )
        posts = result.scalars().all()

        logger.info(f"Fetched {len(posts)} posts out of {total} total")
        return list(posts), total

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

        # 검색어를 tsquery로 변환
        search_query = ' & '.join(query.strip().split())

        # ts_rank를 사용한 전문 검색 쿼리
        search_sql = text("""
            SELECT
                posts.id, posts.title, posts.content, posts.author,
                posts.original_url, posts.normalized_url, posts.blog_id,
                posts.published_at, posts.created_at, posts.updated_at,
                blogs.id as blog_id, blogs.name as blog_name,
                blogs.company as blog_company, blogs.site_url as blog_site_url,
                blogs.logo_url as blog_logo_url,
                ts_rank(posts.keyword_vector, to_tsquery('simple', :search_query)) as rank
            FROM posts
            JOIN blogs ON posts.blog_id = blogs.id
            WHERE posts.keyword_vector @@ to_tsquery('simple', :search_query)
            ORDER BY rank DESC, posts.published_at DESC NULLS LAST
            LIMIT :limit
            OFFSET :offset
        """)

        # 검색 실행
        result = await self.db.execute(
            search_sql,
            {
                "search_query": search_query,
                "limit": limit,
                "offset": offset
            }
        )
        rows = result.fetchall()

        # 총 개수 조회
        count_sql = text("""
            SELECT COUNT(*) as total
            FROM posts
            WHERE posts.keyword_vector @@ to_tsquery('simple', :search_query)
        """)

        count_result = await self.db.execute(count_sql, {"search_query": search_query})
        total = count_result.scalar()

        # 결과 변환
        search_results = []
        for row in rows:
            blog_info = BlogInfo(
                id=row.blog_id,
                name=row.blog_name,
                company=row.blog_company,
                site_url=row.blog_site_url,
                logo_url=row.blog_logo_url
            )

            post_dict = {
                "id": row.id,
                "title": row.title,
                "content": row.content,
                "author": row.author,
                "original_url": row.original_url,
                "normalized_url": row.normalized_url,
                "blog_id": row.blog_id,
                "published_at": row.published_at,
                "created_at": row.created_at,
                "updated_at": row.updated_at,
                "keywords": [],
                "blog": blog_info,
                "rank": float(row.rank)
            }
            search_results.append(post_dict)

        logger.info(f"Found {len(search_results)} posts out of {total} total for query '{query}'")
        return search_results, total or 0

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

        query = select(Post).where(
            (Post.original_url == original_url) |
            (Post.normalized_url == normalized_url)
        )

        if exclude_id:
            query = query.where(Post.id != exclude_id)

        result = await self.db.execute(query)
        duplicate = result.scalar_one_or_none()

        if duplicate:
            logger.warning(f"Duplicate post URL found: id={duplicate.id}")
            return True

        logger.info("No duplicate URL found")
        return False

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
        blog_result = await self.db.execute(select(Blog).where(Blog.id == post_data.blog_id))
        if not blog_result.scalar_one_or_none():
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
        self.db.add(new_post)
        await self.db.commit()
        await self.db.refresh(new_post)

        logger.info(f"Post created successfully: id={new_post.id}, title={new_post.title}")
        return new_post

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

        await self.db.commit()
        await self.db.refresh(post)

        logger.info(f"Post updated successfully: id={post_id}")
        return post

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

        await self.db.delete(post)
        await self.db.commit()

        logger.info(f"Post deleted successfully: id={post_id}")
