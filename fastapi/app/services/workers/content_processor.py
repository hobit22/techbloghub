"""
본문 추출 처리 서비스
PENDING 상태의 Post들을 조회하여 본문 추출
"""

import asyncio
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

    async def process_post(self, post: Post, skip_commit: bool = False) -> dict:
        """
        단일 포스트 본문 추출

        Args:
            post: Post 모델
            skip_commit: True면 커밋하지 않음 (배치 처리용)

        Returns:
            처리 결과 dict
        """
        result = {
            'post_id': post.id,
            'url': post.original_url,
            'status': 'unknown',
            'error': None,
            'success': False
        }

        try:
            # 상태 변경: PROCESSING
            post.status = PostStatus.PROCESSING

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
                result['success'] = True
                result['content_length'] = len(post.content) if post.content else 0

            # skip_commit=False인 경우만 커밋 (단일 처리용)
            if not skip_commit:
                await self.db.commit()

        except Exception as e:
            # 예외 발생 시
            post.status = PostStatus.FAILED
            post.retry_count += 1
            post.error_message = str(e)[:500]  # 에러 메시지 저장 (500자 제한)

            if not skip_commit:
                await self.db.commit()

            result['status'] = 'error'
            result['error'] = str(e)

        return result

    async def process_pending_batch(
        self,
        batch_size: int = 10,
        max_concurrent: int = 5
    ) -> dict:
        """
        대기 중인 포스트들을 배치로 병렬 처리

        Args:
            batch_size: 한 번에 처리할 개수
            max_concurrent: 최대 동시 처리 개수 (기본: 5, Playwright 메모리 고려)

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

        # Semaphore로 동시 처리 제한 (Playwright 메모리 부하 고려)
        semaphore = asyncio.Semaphore(max_concurrent)

        async def process_with_limit(post: Post):
            async with semaphore:
                # skip_commit=True: 개별 커밋하지 않음
                return await self.process_post(post, skip_commit=True)

        # 병렬 처리 (예외 발생 시 Exception 객체 반환)
        results = await asyncio.gather(
            *[process_with_limit(post) for post in pending_posts],
            return_exceptions=True
        )

        # 결과 집계
        for result in results:
            if isinstance(result, Exception):
                # 예외가 발생한 경우
                summary['total_processed'] += 1
                summary['failed'] += 1
                summary['errors'].append({
                    'post_id': None,
                    'url': None,
                    'error': str(result)
                })
                logger.error(f"Unexpected error during batch processing: {result}")
            else:
                # 정상 처리된 경우
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

        # 모든 변경사항 한 번에 커밋
        try:
            await self.db.commit()
            logger.info(f"Batch commit successful: {summary['total_processed']} posts processed")
        except Exception as e:
            await self.db.rollback()
            logger.error(f"Batch commit failed: {e}")
            raise

        return summary

    async def retry_failed_posts(
        self,
        batch_size: int = 5,
        max_concurrent: int = 3
    ) -> dict:
        """
        실패한 포스트들 병렬 재시도

        Args:
            batch_size: 한 번에 재시도할 개수
            max_concurrent: 최대 동시 처리 개수 (기본: 3, 재시도는 더 보수적으로)

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

        # Semaphore로 동시 처리 제한 (재시도는 더 보수적으로)
        semaphore = asyncio.Semaphore(max_concurrent)

        async def retry_with_limit(post: Post):
            async with semaphore:
                # skip_commit=True: 개별 커밋하지 않음
                result = await self.process_post(post, skip_commit=True)
                # retry_count는 process_post 안에서 증가됨
                return result, post.retry_count

        # 병렬 재시도
        results = await asyncio.gather(
            *[retry_with_limit(post) for post in failed_posts],
            return_exceptions=True
        )

        # 결과 집계
        for result_tuple in results:
            if isinstance(result_tuple, Exception):
                # 예외가 발생한 경우
                summary['total_retried'] += 1
                summary['failed'] += 1
                summary['errors'].append({
                    'post_id': None,
                    'url': None,
                    'retry_count': None,
                    'error': str(result_tuple)
                })
                logger.error(f"Unexpected error during retry: {result_tuple}")
            else:
                # 정상 처리된 경우
                result, retry_count = result_tuple
                summary['total_retried'] += 1

                if result['status'] == 'completed':
                    summary['completed'] += 1
                else:
                    summary['failed'] += 1
                    summary['errors'].append({
                        'post_id': result['post_id'],
                        'url': result['url'],
                        'retry_count': retry_count,
                        'error': result.get('error')
                    })

        # 모든 변경사항 한 번에 커밋
        try:
            await self.db.commit()
            logger.info(f"Retry batch commit successful: {summary['total_retried']} posts retried")
        except Exception as e:
            await self.db.rollback()
            logger.error(f"Retry batch commit failed: {e}")
            raise

        return summary

    async def get_processing_stats(self) -> dict:
        """
        본문 추출 처리 통계 조회

        Returns:
            상태별 포스트 개수
        """
        return await self.repository.get_processing_stats()
