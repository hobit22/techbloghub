"""
Blog Repository
블로그 모델에 대한 데이터베이스 접근 로직
"""

import logging
from typing import List, Optional, Dict
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, or_

from app.models.blog import Blog, BlogStatus
from app.models.post import Post

logger = logging.getLogger(__name__)


class BlogRepository:
    """블로그 데이터베이스 접근 계층"""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, blog_id: int) -> Optional[Blog]:
        """
        ID로 블로그 조회

        Args:
            blog_id: 블로그 ID

        Returns:
            Blog 또는 None
        """
        result = await self.db.execute(select(Blog).where(Blog.id == blog_id))
        return result.scalar_one_or_none()

    async def get_all(
        self,
        skip: int = 0,
        limit: int = 20,
        order_by: str = "created_at",
        desc: bool = True
    ) -> tuple[List[Blog], int]:
        """
        블로그 목록 조회 (페이지네이션)

        Args:
            skip: 건너뛸 개수
            limit: 최대 조회 개수
            order_by: 정렬 기준 컬럼
            desc: 내림차순 여부

        Returns:
            (블로그 리스트, 전체 개수)
        """
        # 총 개수 조회
        total_result = await self.db.execute(select(func.count(Blog.id)))
        total = total_result.scalar()

        # 정렬 기준 설정
        order_column = getattr(Blog, order_by, Blog.created_at)
        order_expr = order_column.desc() if desc else order_column.asc()

        # 블로그 목록 조회
        result = await self.db.execute(
            select(Blog)
            .order_by(order_expr)
            .offset(skip)
            .limit(limit)
        )
        blogs = result.scalars().all()

        return list(blogs), total

    async def get_by_status(self, status: BlogStatus) -> List[Blog]:
        """
        상태별 블로그 목록 조회

        Args:
            status: 블로그 상태

        Returns:
            블로그 리스트
        """
        result = await self.db.execute(
            select(Blog)
            .where(Blog.status == status)
            .order_by(Blog.created_at.desc())
        )
        return list(result.scalars().all())

    async def get_active(self) -> List[Blog]:
        """
        활성 상태의 블로그 목록 조회

        Returns:
            활성 블로그 리스트
        """
        return await self.get_by_status(BlogStatus.ACTIVE)

    async def exists_by_name(self, name: str, exclude_id: Optional[int] = None) -> bool:
        """
        이름으로 블로그 존재 여부 확인

        Args:
            name: 블로그 이름
            exclude_id: 제외할 블로그 ID (수정 시 사용)

        Returns:
            존재하면 True, 아니면 False
        """
        query = select(Blog).where(Blog.name == name)

        if exclude_id:
            query = query.where(Blog.id != exclude_id)

        result = await self.db.execute(query)
        return result.scalar_one_or_none() is not None

    async def exists_by_rss_url(self, rss_url: str, exclude_id: Optional[int] = None) -> bool:
        """
        RSS URL로 블로그 존재 여부 확인

        Args:
            rss_url: RSS URL
            exclude_id: 제외할 블로그 ID (수정 시 사용)

        Returns:
            존재하면 True, 아니면 False
        """
        query = select(Blog).where(Blog.rss_url == rss_url)

        if exclude_id:
            query = query.where(Blog.id != exclude_id)

        result = await self.db.execute(query)
        return result.scalar_one_or_none() is not None

    async def exists_duplicate(
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
        conditions = []

        if name:
            conditions.append(Blog.name == name)
        if rss_url:
            conditions.append(Blog.rss_url == rss_url)

        if not conditions:
            return False

        # OR 조건으로 결합
        query = select(Blog).where(or_(*conditions))

        # 제외할 ID가 있으면 추가
        if exclude_id:
            query = query.where(Blog.id != exclude_id)

        result = await self.db.execute(query)
        return result.scalar_one_or_none() is not None

    async def create(self, blog: Blog) -> Blog:
        """
        새로운 블로그 생성

        Args:
            blog: Blog 인스턴스

        Returns:
            생성된 Blog
        """
        self.db.add(blog)
        await self.db.commit()
        await self.db.refresh(blog)
        return blog

    async def update(self, blog: Blog) -> Blog:
        """
        블로그 수정

        Args:
            blog: 수정할 Blog 인스턴스

        Returns:
            수정된 Blog
        """
        await self.db.commit()
        await self.db.refresh(blog)
        return blog

    async def delete(self, blog: Blog) -> None:
        """
        블로그 삭제

        Args:
            blog: 삭제할 Blog 인스턴스
        """
        await self.db.delete(blog)
        await self.db.commit()

    async def update_crawl_status(
        self,
        blog_id: int,
        success: bool,
        increment_failure: bool = False
    ) -> Optional[Blog]:
        """
        크롤링 상태 업데이트

        Args:
            blog_id: 블로그 ID
            success: 성공 여부
            increment_failure: 실패 횟수 증가 여부

        Returns:
            업데이트된 Blog 또는 None
        """
        blog = await self.get_by_id(blog_id)
        if not blog:
            return None

        if success:
            blog.mark_crawl_success()
        elif increment_failure:
            blog.mark_crawl_failure()

        await self.db.commit()
        await self.db.refresh(blog)
        return blog

    async def get_post_stats(self, blog_ids: Optional[List[int]] = None) -> Dict[int, Dict]:
        """
        블로그별 포스트 통계 조회

        Args:
            blog_ids: 특정 블로그 ID 리스트 (None이면 전체 블로그)

        Returns:
            {blog_id: {"post_count": int, "latest_post_published_at": datetime}} 형태의 딕셔너리
        """
        query = (
            select(
                Post.blog_id,
                func.count(Post.id).label('post_count'),
                func.max(Post.published_at).label('latest_post_published_at')
            )
            .group_by(Post.blog_id)
        )

        if blog_ids:
            query = query.where(Post.blog_id.in_(blog_ids))

        result = await self.db.execute(query)
        rows = result.all()

        stats = {}
        for row in rows:
            stats[row.blog_id] = {
                "post_count": row.post_count,
                "latest_post_published_at": row.latest_post_published_at
            }

        return stats

    async def get_status_stats(self) -> Dict[str, int]:
        """
        블로그 상태별 통계 조회

        Returns:
            상태별 개수 딕셔너리
        """
        result = await self.db.execute(
            select(Blog.status, func.count(Blog.id))
            .group_by(Blog.status)
        )

        stats = {status.value: 0 for status in BlogStatus}
        for status, count in result.fetchall():
            stats[status.value] = count

        return stats
