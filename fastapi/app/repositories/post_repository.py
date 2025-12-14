"""
Post Repository
포스트 모델에 대한 데이터베이스 접근 로직
"""

import logging
from typing import List, Optional, Dict, Any
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, or_, text
from sqlalchemy.orm import selectinload

from app.models.post import Post, PostStatus

logger = logging.getLogger(__name__)


class PostRepository:
    """포스트 데이터베이스 접근 계층"""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(
        self,
        post_id: int,
        include_blog: bool = False
    ) -> Optional[Post]:
        """
        ID로 포스트 조회

        Args:
            post_id: 포스트 ID
            include_blog: 블로그 정보 포함 여부

        Returns:
            Post 또는 None
        """
        query = select(Post).where(Post.id == post_id)

        if include_blog:
            query = query.options(selectinload(Post.blog))

        result = await self.db.execute(query)
        return result.scalar_one_or_none()

    async def get_all(
        self,
        skip: int = 0,
        limit: int = 20,
        blog_id: Optional[int] = None,
        include_blog: bool = False,
        order_by: str = "published_at",
        desc: bool = True
    ) -> tuple[List[Post], int]:
        """
        포스트 목록 조회 (페이지네이션)

        Args:
            skip: 건너뛸 개수
            limit: 최대 조회 개수
            blog_id: 특정 블로그 필터링
            include_blog: 블로그 정보 포함 여부
            order_by: 정렬 기준 컬럼
            desc: 내림차순 여부

        Returns:
            (포스트 리스트, 전체 개수)
        """
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

        # 정렬 기준 설정
        order_column = getattr(Post, order_by, Post.published_at)
        if order_by == "published_at":
            # published_at은 NULL을 마지막에 배치
            order_expr = order_column.desc().nulls_last() if desc else order_column.asc().nulls_last()
        else:
            order_expr = order_column.desc() if desc else order_column.asc()

        # 포스트 목록
        result = await self.db.execute(
            query
            .order_by(order_expr, Post.created_at.desc())
            .offset(skip)
            .limit(limit)
        )
        posts = result.scalars().all()

        return list(posts), total

    async def get_by_status(
        self,
        status: PostStatus,
        limit: int = 10,
        max_retry_count: Optional[int] = None
    ) -> List[Post]:
        """
        상태별 포스트 목록 조회

        Args:
            status: 포스트 상태
            limit: 최대 조회 개수
            max_retry_count: 최대 재시도 횟수 (None이면 필터링 안함)

        Returns:
            포스트 리스트
        """
        query = select(Post).where(Post.status == status)

        if max_retry_count is not None:
            query = query.where(Post.retry_count < max_retry_count)

        # 상태에 따라 정렬 기준 변경
        if status == PostStatus.PENDING:
            query = query.order_by(Post.published_at.desc())  # 최신 것부터
        elif status == PostStatus.FAILED:
            query = query.order_by(Post.updated_at.asc())  # 오래된 것부터 재시도

        result = await self.db.execute(query.limit(limit))
        return list(result.scalars().all())

    async def get_pending_posts(
        self,
        limit: int = 10,
        max_retry_count: int = 3
    ) -> List[Post]:
        """
        본문 추출 대기 중인 Post 조회

        Args:
            limit: 최대 조회 개수
            max_retry_count: 최대 재시도 횟수

        Returns:
            PENDING 상태 Post 리스트
        """
        return await self.get_by_status(
            status=PostStatus.PENDING,
            limit=limit,
            max_retry_count=max_retry_count
        )

    async def get_failed_posts(
        self,
        limit: int = 10,
        max_retry_count: int = 3
    ) -> List[Post]:
        """
        본문 추출 실패한 Post 조회 (재시도 가능)

        Args:
            limit: 최대 조회 개수
            max_retry_count: 최대 재시도 횟수

        Returns:
            FAILED 상태이지만 재시도 횟수 미만인 Post 리스트
        """
        return await self.get_by_status(
            status=PostStatus.FAILED,
            limit=limit,
            max_retry_count=max_retry_count
        )

    async def get_existing_urls(self, blog_id: int) -> set[str]:
        """
        특정 블로그의 이미 수집된 URL 조회 (중복 방지)

        Args:
            blog_id: 블로그 ID

        Returns:
            기존 URL 집합
        """
        result = await self.db.execute(
            select(Post.original_url).where(Post.blog_id == blog_id)
        )
        return {row[0] for row in result.fetchall()}

    async def exists_by_url(
        self,
        original_url: str,
        normalized_url: str,
        exclude_id: Optional[int] = None
    ) -> bool:
        """
        포스트 URL 존재 여부 확인

        Args:
            original_url: 원본 URL
            normalized_url: 정규화된 URL
            exclude_id: 제외할 포스트 ID

        Returns:
            존재하면 True, 아니면 False
        """
        query = select(Post).where(
            or_(
                Post.original_url == original_url,
                Post.normalized_url == normalized_url
            )
        )

        if exclude_id:
            query = query.where(Post.id != exclude_id)

        result = await self.db.execute(query)
        return result.scalar_one_or_none() is not None

    async def search_fulltext(
        self,
        search_query: str,
        limit: int = 20,
        offset: int = 0
    ) -> tuple[List[Dict[str, Any]], int]:
        """
        포스트 전문 검색 (PostgreSQL Full-Text Search)

        Args:
            search_query: 검색어 (공백으로 구분된 단어들은 자동으로 '&' 결합)
            limit: 최대 결과 개수
            offset: 건너뛸 개수

        Returns:
            (검색 결과 리스트, 전체 개수)
        """
        # 검색어를 tsquery로 변환
        tsquery = ' & '.join(search_query.strip().split())

        # ts_rank를 사용한 전문 검색 쿼리
        search_sql = text("""
            SELECT
                posts.id, posts.title, posts.content, posts.author,
                posts.original_url, posts.normalized_url, posts.blog_id,
                posts.published_at, posts.created_at, posts.updated_at,
                blogs.id as blog_id, blogs.name as blog_name,
                blogs.company as blog_company, blogs.site_url as blog_site_url,
                blogs.logo_url as blog_logo_url,
                ts_rank(posts.keyword_vector, to_tsquery('simple', :tsquery)) as rank
            FROM posts
            JOIN blogs ON posts.blog_id = blogs.id
            WHERE posts.keyword_vector @@ to_tsquery('simple', :tsquery)
            ORDER BY rank DESC, posts.published_at DESC NULLS LAST
            LIMIT :limit
            OFFSET :offset
        """)

        # 검색 실행
        result = await self.db.execute(
            search_sql,
            {"tsquery": tsquery, "limit": limit, "offset": offset}
        )
        rows = result.fetchall()

        # 총 개수 조회
        count_sql = text("""
            SELECT COUNT(*) as total
            FROM posts
            WHERE posts.keyword_vector @@ to_tsquery('simple', :tsquery)
        """)

        count_result = await self.db.execute(count_sql, {"tsquery": tsquery})
        total = count_result.scalar() or 0

        # 결과 변환
        search_results = []
        for row in rows:
            search_results.append({
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
                "blog_name": row.blog_name,
                "blog_company": row.blog_company,
                "blog_site_url": row.blog_site_url,
                "blog_logo_url": row.blog_logo_url,
                "rank": float(row.rank)
            })

        return search_results, total

    async def get_processing_stats(self) -> Dict[str, int]:
        """
        포스트 처리 상태 통계 조회

        Returns:
            상태별 개수 딕셔너리
        """
        result = await self.db.execute(
            select(Post.status, func.count(Post.id))
            .group_by(Post.status)
        )

        stats = {status.value: 0 for status in PostStatus}
        for status, count in result.fetchall():
            stats[status.value] = count

        return stats

    async def create(self, post: Post) -> Post:
        """
        새로운 포스트 생성

        Args:
            post: Post 인스턴스

        Returns:
            생성된 Post
        """
        self.db.add(post)
        await self.db.commit()
        await self.db.refresh(post)
        return post

    async def update(self, post: Post) -> Post:
        """
        포스트 수정

        Args:
            post: 수정할 Post 인스턴스

        Returns:
            수정된 Post
        """
        await self.db.commit()
        await self.db.refresh(post)
        return post

    async def delete(self, post: Post) -> None:
        """
        포스트 삭제

        Args:
            post: 삭제할 Post 인스턴스
        """
        await self.db.delete(post)
        await self.db.commit()

    async def bulk_create(self, posts: List[Post]) -> List[Post]:
        """
        여러 포스트 일괄 생성

        Args:
            posts: Post 인스턴스 리스트

        Returns:
            생성된 Post 리스트
        """
        self.db.add_all(posts)
        await self.db.commit()

        # refresh all
        for post in posts:
            await self.db.refresh(post)

        return posts
