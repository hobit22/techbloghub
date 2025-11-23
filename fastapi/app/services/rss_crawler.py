"""
RSS 크롤링 서비스
Trafilatura를 사용한 RSS 피드 파싱 및 URL 추출
"""

from typing import List, Optional
from urllib.parse import urlparse, quote
from datetime import datetime

from trafilatura import feeds, fetch_url
from trafilatura.feeds import FeedParameters
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.config import settings
from app.models.blog import Blog
from app.models.post import Post
from app.services.content_extractor import ContentExtractor


class RSSCrawler:
    """RSS 크롤링 서비스"""

    def __init__(self, db: AsyncSession):
        self.db = db
        self.proxy_url = settings.RSS_PROXY_URL
        self.content_extractor = ContentExtractor()

    def get_domain_from_url(self, url: str) -> str:
        """URL에서 도메인 추출"""
        parsed = urlparse(url)
        return parsed.netloc

    def extract_rss_urls(self, rss_url: str) -> List[str]:
        """
        RSS 피드에서 URL 추출

        Args:
            rss_url: RSS 피드 URL

        Returns:
            추출된 URL 리스트
        """
        try:
            # Cloudflare 프록시를 통해 RSS 다운로드
            proxy_url = self.proxy_url + quote(rss_url, safe='')
            feed_content = fetch_url(proxy_url)

            if not feed_content:
                return []

            # 도메인 추출 및 FeedParameters 생성
            domain = self.get_domain_from_url(rss_url)
            base_url = f"{urlparse(rss_url).scheme}://{domain}"

            params = FeedParameters(
                baseurl=base_url,
                domain=domain,
                reference=rss_url,
                external=True,  # 모든 링크 허용 (cross-domain)
                target_lang='ko'
            )

            # 링크 추출
            urls = feeds.extract_links(feed_content, params)
            return urls if urls else []

        except Exception as e:
            print(f"RSS URL 추출 실패 ({rss_url}): {e}")
            return []

    async def get_existing_urls(self, blog_id: int) -> set[str]:
        """
        이미 크롤링된 URL 조회 (중복 방지)

        Args:
            blog_id: 블로그 ID

        Returns:
            기존 URL 집합
        """
        result = await self.db.execute(
            select(Post.original_url).where(Post.blog_id == blog_id)
        )
        return {row[0] for row in result.fetchall()}

    async def crawl_blog(
        self,
        blog: Blog,
        max_posts: Optional[int] = None
    ) -> dict:
        """
        단일 블로그 크롤링

        Args:
            blog: 블로그 모델
            max_posts: 최대 크롤링 개수 (None이면 전체)

        Returns:
            {
                'blog_id': int,
                'blog_name': str,
                'total_urls': int,
                'new_posts': int,
                'failed_posts': int,
                'errors': List[str]
            }
        """
        result = {
            'blog_id': blog.id,
            'blog_name': blog.name,
            'total_urls': 0,
            'new_posts': 0,
            'failed_posts': 0,
            'errors': []
        }

        try:
            # 1. RSS에서 URL 추출
            urls = self.extract_rss_urls(blog.rss_url)
            result['total_urls'] = len(urls)

            if not urls:
                result['errors'].append("No URLs extracted from RSS")
                blog.mark_crawl_failure()
                await self.db.commit()
                return result

            # 2. 기존 URL 조회 (중복 방지)
            existing_urls = await self.get_existing_urls(blog.id)

            # 3. 새로운 URL만 필터링
            new_urls = [url for url in urls if url not in existing_urls]

            if not new_urls:
                blog.mark_crawl_success()
                await self.db.commit()
                return result

            # 4. max_posts 제한 적용
            if max_posts:
                new_urls = new_urls[:max_posts]

            # 5. 각 URL에서 본문 추출 및 저장
            for url in new_urls:
                try:
                    # 본문 추출 (Failover 전략)
                    extracted = await self.content_extractor.extract(url, use_proxy=True)

                    if not extracted:
                        result['failed_posts'] += 1
                        result['errors'].append(f"Content extraction failed: {url[:100]}")
                        continue

                    # 발행일 파싱
                    published_at = None
                    if extracted.get('date'):
                        try:
                            published_at = datetime.fromisoformat(extracted['date'])
                        except:
                            pass

                    # Post 생성
                    new_post = Post(
                        title=extracted.get('title') or 'Untitled',
                        content=extracted.get('content'),
                        author=extracted.get('author'),
                        original_url=url,
                        normalized_url=Post.normalize_url(url),
                        blog_id=blog.id,
                        published_at=published_at
                    )

                    self.db.add(new_post)
                    result['new_posts'] += 1

                except Exception as e:
                    result['failed_posts'] += 1
                    result['errors'].append(f"Error processing {url[:100]}: {str(e)[:100]}")

            # 6. 크롤링 성공 마킹
            blog.mark_crawl_success()
            await self.db.commit()

        except Exception as e:
            blog.mark_crawl_failure()
            await self.db.commit()
            result['errors'].append(f"Blog crawl failed: {str(e)}")

        return result

    async def crawl_all_active_blogs(
        self,
        max_posts_per_blog: Optional[int] = None
    ) -> List[dict]:
        """
        모든 활성 블로그 크롤링

        Args:
            max_posts_per_blog: 블로그당 최대 크롤링 개수

        Returns:
            각 블로그별 크롤링 결과 리스트
        """
        # 활성 블로그 조회
        result = await self.db.execute(
            select(Blog).where(Blog.status == "ACTIVE")
        )
        blogs = result.scalars().all()

        results = []
        for blog in blogs:
            result = await self.crawl_blog(blog, max_posts=max_posts_per_blog)
            results.append(result)

        return results
