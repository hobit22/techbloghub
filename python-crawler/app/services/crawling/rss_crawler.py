import feedparser
import httpx
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any
from loguru import logger
from dateutil import parser as date_parser

from ...models import BlogEntity, PostEntity
from ...core.config import settings


class RSSCrawler:
    """RSS 피드 크롤링을 담당하는 서비스"""
    
    def __init__(self):
        self.client = httpx.AsyncClient(
            headers={"User-Agent": settings.crawler_user_agent},
            timeout=settings.crawler_timeout
        )
    
    async def crawl_feed(self, blog: BlogEntity) -> List[PostEntity]:
        """RSS 피드를 크롤링하여 포스트 목록을 반환"""
        try:
            logger.info(f"Crawling RSS feed for blog: {blog.name}")
            
            # RSS 피드 가져오기
            response = await self.client.get(blog.rss_url)
            response.raise_for_status()
            
            # feedparser로 파싱
            feed = feedparser.parse(response.content)
            
            if feed.bozo:
                logger.warning(f"Feed parsing warning for {blog.name}: {feed.bozo_exception}")
            
            posts = []
            for entry in feed.entries[:settings.crawler_max_posts_per_feed]:
                try:
                    post = self._create_post_from_entry(entry, blog)
                    if post:
                        posts.append(post)
                except Exception as e:
                    logger.error(f"Error processing entry from {blog.name}: {e}")
                    continue
            
            logger.info(f"Crawled {len(posts)} posts from {blog.name}")
            return posts
            
        except httpx.RequestError as e:
            logger.error(f"Request error crawling {blog.name}: {e}")
            return []
        except Exception as e:
            logger.error(f"Error crawling RSS feed for blog {blog.name}: {e}")
            return []
    
    def _create_post_from_entry(self, entry: Dict[Any, Any], blog: BlogEntity) -> Optional[PostEntity]:
        """RSS 엔트리에서 포스트 엔티티를 생성"""
        try:
            title = self._clean_title(getattr(entry, 'title', ''))
            content = self._extract_content(entry)
            url = getattr(entry, 'link', '')
            author = self._extract_author(entry)
            published_at = self._extract_published_date(entry)
            
            if not title or not url:
                logger.warning("Skipping entry with missing title or URL")
                return None
            
            return PostEntity(
                title=title,
                content=content,
                original_url=url,
                author=author,
                published_at=published_at,
                blog_id=blog.id
            )
            
        except Exception as e:
            logger.error(f"Error creating post from entry: {e}")
            return None
    
    def _clean_title(self, title: str) -> str:
        """HTML 태그를 제거하고 제목을 정리"""
        if not title:
            return ""
        
        # 간단한 HTML 태그 제거
        import re
        clean_title = re.sub(r'<[^>]*>', '', title)
        return clean_title.strip()
    
    def _extract_content(self, entry: Dict[Any, Any]) -> Optional[str]:
        """RSS 엔트리에서 콘텐츠 추출"""
        content = None
        
        # description 또는 summary 시도
        if hasattr(entry, 'description'):
            content = entry.description
        elif hasattr(entry, 'summary'):
            content = entry.summary
        
        # content 리스트에서 추출 시도
        if not content and hasattr(entry, 'content') and entry.content:
            content = entry.content[0].get('value', '')
        
        if content:
            # HTML 태그 제거
            import re
            content = re.sub(r'<[^>]*>', '', content)
            content = content.strip()
            
            # 길이 제한 (1000자)
            if len(content) > 1000:
                content = content[:1000] + "..."
        
        return content
    
    def _extract_author(self, entry: Dict[Any, Any]) -> Optional[str]:
        """RSS 엔트리에서 작성자 추출"""
        if hasattr(entry, 'author') and entry.author:
            return entry.author.strip()
        
        if hasattr(entry, 'authors') and entry.authors:
            return entry.authors[0].get('name', '').strip()
        
        return None
    
    def _extract_published_date(self, entry: Dict[Any, Any]) -> Optional[datetime]:
        """RSS 엔트리에서 발행일 추출"""
        # published 시도
        if hasattr(entry, 'published_parsed') and entry.published_parsed:
            try:
                return datetime(*entry.published_parsed[:6], tzinfo=timezone.utc)
            except (ValueError, TypeError):
                pass
        
        if hasattr(entry, 'published') and entry.published:
            try:
                return date_parser.parse(entry.published)
            except (ValueError, TypeError):
                pass
        
        # updated 시도
        if hasattr(entry, 'updated_parsed') and entry.updated_parsed:
            try:
                return datetime(*entry.updated_parsed[:6], tzinfo=timezone.utc)
            except (ValueError, TypeError):
                pass
        
        if hasattr(entry, 'updated') and entry.updated:
            try:
                return date_parser.parse(entry.updated)
            except (ValueError, TypeError):
                pass
        
        # 기본값: 현재 시간
        return datetime.now(timezone.utc)
    
    async def close(self):
        """HTTP 클라이언트 종료"""
        await self.client.aclose()