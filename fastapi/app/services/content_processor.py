"""
본문 추출 처리 서비스
PENDING 상태의 Post들을 조회하여 본문 추출
"""

from typing import List

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.models.post import Post, PostStatus
from app.services.content_extractor import ContentExtractor


class ContentProcessor:
    """본문 추출 배치 처리"""

    def __init__(self, db: AsyncSession):
        self.db = db
        self.extractor = ContentExtractor()
        self.max_retries = 3

    async def get_pending_posts(self, limit: int = 10) -> List[Post]:
        """
        본문 추출 대기 중인 Post 조회

        Args:
            limit: 최대 조회 개수

        Returns:
            PENDING 상태 Post 리스트
        """
        result = await self.db.execute(
            select(Post)
            .where(
                Post.status == PostStatus.PENDING,
                Post.retry_count < self.max_retries
            )
            .order_by(Post.created_at.asc())  # 오래된 것부터
            .limit(limit)
        )
        return result.scalars().all()

    async def get_failed_posts(self, limit: int = 10) -> List[Post]:
        """
        본문 추출 실패한 Post 조회 (재시도 가능)

        Args:
            limit: 최대 조회 개수

        Returns:
            FAILED 상태이지만 재시도 횟수 미만인 Post 리스트
        """
        result = await self.db.execute(
            select(Post)
            .where(
                Post.status == PostStatus.FAILED,
                Post.retry_count < self.max_retries
            )
            .order_by(Post.updated_at.asc())
            .limit(limit)
        )
        return result.scalars().all()

    async def process_post(self, post: Post) -> dict:
        """
        단일 포스트 본문 추출

        Args:
            post: Post 모델

        Returns:
            처리 결과 dict
        """
        result = {
            'post_id': post.id,
            'url': post.original_url,
            'status': 'unknown',
            'error': None
        }

        try:
            # 상태 변경: PROCESSING
            post.status = PostStatus.PROCESSING
            await self.db.commit()

            # 본문 추출
            extracted = await self.extractor.extract(post.original_url, use_proxy=True)

            if not extracted:
                # 추출 실패
                post.status = PostStatus.FAILED
                post.retry_count += 1
                post.error_message = "Content extraction returned None"
                result['status'] = 'failed'
                result['error'] = 'extraction_failed'

            else:
                # 추출 성공
                post.content = extracted.get('content')
                post.author = extracted.get('author')

                # RSS 날짜를 우선 사용 (trafilatura의 날짜는 신뢰할 수 없음)
                # RSS에서 이미 published_at이 설정되어 있으면 절대 변경하지 않음
                if not post.published_at:
                    # RSS에 날짜가 없었던 경우에만 경고 로그 출력
                    print(f"WARNING: Post {post.id} has no published_at from RSS. "
                          f"Trafilatura extracted date: {extracted.get('date')}, "
                          f"but NOT using it (unreliable).")

                post.status = PostStatus.COMPLETED
                post.error_message = None
                result['status'] = 'completed'
                result['content_length'] = len(post.content) if post.content else 0

            await self.db.commit()

        except Exception as e:
            # 예외 발생 시
            post.status = PostStatus.FAILED
            post.retry_count += 1
            post.error_message = str(e)[:500]  # 에러 메시지 저장 (500자 제한)
            await self.db.commit()

            result['status'] = 'error'
            result['error'] = str(e)

        return result

    async def process_pending_batch(
        self,
        batch_size: int = 10
    ) -> dict:
        """
        대기 중인 포스트들을 배치로 처리

        Args:
            batch_size: 한 번에 처리할 개수

        Returns:
            {
                'total_processed': int,
                'completed': int,
                'failed': int,
                'errors': List[dict]
            }
        """
        summary = {
            'total_processed': 0,
            'completed': 0,
            'failed': 0,
            'errors': []
        }

        # PENDING 상태 포스트 조회
        pending_posts = await self.get_pending_posts(limit=batch_size)

        if not pending_posts:
            return summary

        # 각 포스트 처리
        for post in pending_posts:
            result = await self.process_post(post)
            summary['total_processed'] += 1

            if result['status'] == 'completed':
                summary['completed'] += 1
            else:
                summary['failed'] += 1
                summary['errors'].append({
                    'post_id': result['post_id'],
                    'url': result['url'],
                    'error': result.get('error')
                })

        return summary

    async def retry_failed_posts(
        self,
        batch_size: int = 5
    ) -> dict:
        """
        실패한 포스트들 재시도

        Args:
            batch_size: 한 번에 재시도할 개수

        Returns:
            처리 결과 요약
        """
        summary = {
            'total_retried': 0,
            'completed': 0,
            'failed': 0,
            'errors': []
        }

        # FAILED 상태이지만 재시도 가능한 포스트 조회
        failed_posts = await self.get_failed_posts(limit=batch_size)

        if not failed_posts:
            return summary

        # 각 포스트 재시도
        for post in failed_posts:
            result = await self.process_post(post)
            summary['total_retried'] += 1

            if result['status'] == 'completed':
                summary['completed'] += 1
            else:
                summary['failed'] += 1
                summary['errors'].append({
                    'post_id': result['post_id'],
                    'url': result['url'],
                    'retry_count': post.retry_count,
                    'error': result.get('error')
                })

        return summary

    async def get_processing_stats(self) -> dict:
        """
        본문 추출 처리 통계 조회

        Returns:
            상태별 포스트 개수
        """
        from sqlalchemy import func

        stats = {}

        # 각 상태별 개수
        for status in PostStatus:
            result = await self.db.execute(
                select(func.count(Post.id)).where(Post.status == status)
            )
            stats[status.value] = result.scalar()

        return stats
