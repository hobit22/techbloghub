"""
RSS 수집 서비스
RSS 피드에서 URL과 메타데이터만 수집 (본문 추출 제외)
"""

from typing import List, Dict, Optional
from urllib.parse import urlparse, quote
from datetime import datetime, timezone
import asyncio
import logging

import feedparser
import httpx
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings

logger = logging.getLogger(__name__)
from app.core.database import AsyncSessionLocal
from app.models.blog import Blog
from app.models.post import Post, PostStatus
from app.repositories import PostRepository, BlogRepository


class RSSCollector:
    """RSS 피드에서 URL과 메타데이터만 수집"""

    def __init__(self, db: AsyncSession):
        self.db = db
        self.proxy_url = settings.RSS_PROXY_URL
        self.post_repository = PostRepository(db)
        self.blog_repository = BlogRepository(db)

    async def extract_rss_entries(self, rss_url: str) -> List[Dict[str, str]]:
        """
        RSS 피드에서 URL과 Title 추출

        Args:
            rss_url: RSS 피드 URL

        Returns:
            [{'url': str, 'title': str, 'published': str}, ...]
        """
        try:
            # Cloudflare 프록시를 통해 RSS 다운로드 (비동기)
            proxy_url = self.proxy_url + quote(rss_url, safe='')

            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(proxy_url)
                response.raise_for_status()
                feed_content = response.text

            if not feed_content:
                return []

            # feedparser로 RSS 파싱 (CPU 작업이지만 가벼워서 그냥 실행)
            feed = feedparser.parse(feed_content)

            entries = []
            for entry in feed.entries:
                url = entry.get('link', '').strip()
                title = entry.get('title', '').strip()
                published = entry.get('published', '')

                if url and title:
                    entries.append({
                        'url': url,
                        'title': title,
                        'published': published
                    })

            return entries

        except Exception as e:
            logger.error(f"RSS 엔트리 추출 실패 ({rss_url}): {e}")
            return []

    async def get_existing_urls(self, blog_id: int) -> set[str]:
        """
        이미 수집된 URL 조회 (중복 방지)

        Args:
            blog_id: 블로그 ID

        Returns:
            기존 URL 집합
        """
        return await self.post_repository.get_existing_urls(blog_id)

    async def collect_blog(
        self,
        blog: Blog,
        max_posts: Optional[int] = None
    ) -> dict:
        """
        단일 블로그에서 새로운 URL 수집 (본문 추출 없이)

        Args:
            blog: 블로그 모델
            max_posts: 최대 수집 개수 (None이면 전체)

        Returns:
            {
                'blog_id': int,
                'blog_name': str,
                'total_entries': int,
                'new_posts': int,
                'skipped_duplicates': int,
                'errors': List[str]
            }
        """
        # blog 속성을 미리 저장 (rollback 시 접근 불가 방지)
        blog_id = blog.id
        blog_name = blog.name
        result = {
            'blog_id': blog_id,
            'blog_name': blog_name,
            'total_entries': 0,
            'new_posts': 0,
            'skipped_duplicates': 0,
            'errors': []
        }

        try:
            # 1. RSS에서 엔트리(URL + Title) 추출 (비동기)
            entries = await self.extract_rss_entries(blog.rss_url)
            result['total_entries'] = len(entries)

            if not entries:
                result['errors'].append("No entries extracted from RSS")
                blog.mark_crawl_failure()
                await self.db.commit()
                return result

            # 2. 기존 URL 조회 (중복 방지)
            existing_urls = await self.get_existing_urls(blog_id)

            # 3. 새로운 엔트리만 필터링
            new_entries = [entry for entry in entries if entry['url'] not in existing_urls]
            result['skipped_duplicates'] = len(entries) - len(new_entries)

            if not new_entries:
                blog.mark_crawl_success()
                await self.db.commit()
                return result

            # 4. max_posts 제한 적용
            if max_posts:
                new_entries = new_entries[:max_posts]

            # 5. Post 생성 (PENDING 상태, content=None)
            for entry in new_entries:
                url = entry['url']
                rss_title = entry['title']
                rss_published = entry.get('published')

                try:
                    # 발행일 파싱
                    published_at = None
                    if rss_published:
                        try:
                            from dateutil import parser
                            published_at = parser.parse(rss_published)
                        except Exception as parse_error:
                            logger.warning(f"Failed to parse published date for {url[:100]}: "
                                           f"RSS date='{rss_published}', error={str(parse_error)}")

                    # 중복 체크 (normalized_url 기준)
                    normalized_url = Post.normalize_url(url)
                    is_duplicate = await self.post_repository.exists_by_url(
                        original_url=url,
                        normalized_url=normalized_url
                    )
                    if is_duplicate:
                        logger.debug(f"Skipping duplicate URL (normalized): {url[:100]}")
                        result['skipped_duplicates'] += 1
                        continue

                    # Post 생성 (본문 없이 URL만)
                    new_post = Post(
                        title=rss_title,
                        content=None,  # 본문 없음
                        original_url=url,
                        normalized_url=normalized_url,
                        blog_id=blog_id,
                        published_at=published_at,
                        status=PostStatus.PENDING  # 본문 추출 대기
                    )

                    self.db.add(new_post)
                    result['new_posts'] += 1

                except Exception as e:
                    error_msg = str(e)
                    result['errors'].append(f"Error creating post {url[:100]}: {error_msg[:100]}")
                    logger.error(f"Failed to create post {url[:100]}: {error_msg}")

            # 6. 모든 Post 한 번에 커밋
            try:
                await self.db.commit()
                blog.mark_crawl_success()
                await self.db.commit()
            except Exception as e:
                await self.db.rollback()
                result['errors'].append(f"Commit failed: {str(e)}")
                blog.mark_crawl_failure()
                await self.db.commit()

        except Exception as e:
            await self.db.rollback()
            result['errors'].append(f"Blog collection failed: {str(e)}")
            try:
                blog.mark_crawl_failure()
                await self.db.commit()
            except Exception as commit_error:
                logger.warning(f"Failed to mark crawl failure for blog {blog_id}: {commit_error}")

        return result

    async def _collect_blog_with_new_session(
        self,
        blog_id: int,
        max_posts: Optional[int] = None
    ) -> dict:
        """
        새로운 세션을 생성하여 단일 블로그 수집 (병렬 처리용)

        Args:
            blog_id: 블로그 ID
            max_posts: 최대 수집 개수

        Returns:
            수집 결과 딕셔너리
        """
        async with AsyncSessionLocal() as new_db:
            # 새 세션에서 blog 다시 조회
            result = await new_db.execute(select(Blog).where(Blog.id == blog_id))
            blog = result.scalar_one_or_none()

            if not blog:
                return {
                    'blog_id': blog_id,
                    'blog_name': 'Unknown',
                    'total_entries': 0,
                    'new_posts': 0,
                    'skipped_duplicates': 0,
                    'errors': [f'Blog {blog_id} not found']
                }

            # 새 세션으로 collector 생성
            temp_collector = RSSCollector(new_db)
            result = await temp_collector.collect_blog(blog, max_posts=max_posts)

            return result

    async def collect_all_active_blogs(
        self,
        max_posts_per_blog: Optional[int] = None,
        max_concurrent: int = 10
    ) -> List[dict]:
        """
        모든 활성 블로그에서 새로운 URL 수집 (병렬 처리)
        각 블로그마다 독립적인 세션을 사용하여 동시성 문제 해결
        Semaphore를 사용하여 동시 처리 개수를 제한하여 DB 커넥션 부족 방지

        Args:
            max_posts_per_blog: 블로그당 최대 수집 개수
            max_concurrent: 동시 처리할 최대 블로그 개수 (기본: 10)

        Returns:
            각 블로그별 수집 결과 리스트
        """
        # 활성 블로그 조회
        blogs = await self.blog_repository.get_active()

        # Semaphore로 동시 처리 개수 제한 (DB connection pool 보호)
        semaphore = asyncio.Semaphore(max_concurrent)

        async def collect_with_semaphore(blog_id: int):
            """Semaphore를 사용하여 동시 처리 개수 제한"""
            async with semaphore:
                return await self._collect_blog_with_new_session(blog_id, max_posts_per_blog)

        # 모든 블로그를 병렬로 수집 (단, 동시 실행은 max_concurrent개로 제한)
        tasks = [collect_with_semaphore(blog.id) for blog in blogs]
        results = await asyncio.gather(*tasks, return_exceptions=True)

        # Exception이 반환된 경우 에러 로깅
        final_results = []
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                blog_name = blogs[i].name if i < len(blogs) else "Unknown"
                error_result = {
                    'blog_id': blogs[i].id if i < len(blogs) else None,
                    'blog_name': blog_name,
                    'total_entries': 0,
                    'new_posts': 0,
                    'skipped_duplicates': 0,
                    'errors': [f"Exception during collection: {str(result)}"]
                }
                final_results.append(error_result)
                logger.error(f"Blog collection failed for {blog_name}: {result}")
            else:
                final_results.append(result)

        return final_results
