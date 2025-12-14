"""
본문 추출 처리 서비스
PENDING 상태의 Post들을 조회하여 본문 추출
"""

import logging
from typing import List
from datetime import datetime

from sqlalchemy.ext.asyncio import AsyncSession

from app.models.post import Post, PostStatus
from app.repositories import PostRepository
from .content_extractor import ContentExtractor

logger = logging.getLogger(__name__)


class ContentProcessor:
    """본문 추출 배치 처리"""

    def __init__(self, db: AsyncSession):
        self.db = db
        self.extractor = ContentExtractor()
        self.max_retries = 3
        self.repository = PostRepository(db)

    async def get_pending_posts(self, limit: int = 10) -> List[Post]:
        """
        본문 추출 대기 중인 Post 조회

        Args:
            limit: 최대 조회 개수

        Returns:
            PENDING 상태 Post 리스트
        """
        return await self.repository.get_pending_posts(
            limit=limit,
            max_retry_count=self.max_retries
        )

    async def get_failed_posts(self, limit: int = 10) -> List[Post]:
        """
        본문 추출 실패한 Post 조회 (재시도 가능)

        Args:
            limit: 최대 조회 개수

        Returns:
            FAILED 상태이지만 재시도 횟수 미만인 Post 리스트
        """
        return await self.repository.get_failed_posts(
            limit=limit,
            max_retry_count=self.max_retries
        )

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

                # 날짜 처리: RSS 날짜 우선, 없으면 Trafilatura 날짜 사용
                if not post.published_at:
                    # RSS에 날짜가 없는 경우, Trafilatura에서 추출한 날짜 사용
                    extracted_date = extracted.get('date')

                    if extracted_date:
                        try:
                            # 날짜 문자열을 datetime으로 파싱
                            if isinstance(extracted_date, str):
                                from dateutil import parser
                                post.published_at = parser.parse(extracted_date)
                                logger.info(f"Post {post.id}: Using Trafilatura date - {post.published_at}")
                            elif isinstance(extracted_date, datetime):
                                post.published_at = extracted_date
                                logger.info(f"Post {post.id}: Using Trafilatura date - {post.published_at}")
                        except Exception as e:
                            logger.warning(f"Post {post.id}: Failed to parse Trafilatura date '{extracted_date}': {e}")
                    else:
                        logger.warning(f"Post {post.id}: No published_at from RSS and Trafilatura")

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
        return await self.repository.get_processing_stats()
