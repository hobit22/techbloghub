import httpx
import asyncio
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any
from loguru import logger
from dateutil import parser as date_parser
import re
from sqlalchemy.exc import IntegrityError

from ...models import BlogEntity, PostEntity
from ...core.config import settings
from ...core.database import SessionLocal


class WordPressCrawler:
    """WordPress REST API를 사용한 백필링 크롤러"""
    
    def __init__(self):
        self.client = httpx.AsyncClient(
            headers={"User-Agent": settings.crawler_user_agent},
            timeout=httpx.Timeout(30.0),  # 30초 타임아웃
            verify=False  # SSL 검증 비활성화 (테스트용)
        )
        self.posts_per_page = 50
        self.request_delay = 1.0  # Rate limiting (초)
    
    async def backfill_all_posts(self, blog: BlogEntity) -> List[PostEntity]:
        """WordPress 블로그의 모든 포스트를 백필링"""
        logger.info(f"Starting backfill for blog: {blog.name} ({blog.site_url})")
        
        try:
            # 전체 포스트 개수 조회
            total_count = await self._get_total_post_count(blog)
            if total_count == 0:
                logger.warning(f"No posts found for blog: {blog.name}")
                return []
            
            logger.info(f"Total posts to crawl: {total_count} for blog: {blog.name}")
            
            # 페이지 수 계산
            total_pages = (total_count + self.posts_per_page - 1) // self.posts_per_page
            
            all_posts = []
            
            # 각 페이지를 순차적으로 크롤링 (Rate limiting)
            for page in range(total_pages):
                offset = page * self.posts_per_page
                logger.info(f"Crawling page {page + 1}/{total_pages} for blog: {blog.name} (offset: {offset})")
                
                try:
                    posts = await self._crawl_posts_page(blog, offset)
                    all_posts.extend(posts)
                    
                    # Rate limiting - 마지막 페이지가 아니면 대기
                    if page < total_pages - 1:
                        await asyncio.sleep(self.request_delay)
                        
                except Exception as e:
                    logger.error(f"Error crawling page {page + 1} for blog {blog.name}: {e}")
                    continue
            
            logger.info(f"Completed backfill for blog: {blog.name}. Total posts: {len(all_posts)}")
            return all_posts
            
        except Exception as e:
            logger.error(f"Error during backfill for blog: {blog.name}: {e}")
            return []
    
    async def _get_total_post_count(self, blog: BlogEntity) -> int:
        """전체 포스트 개수 조회"""
        api_url = f"{blog.site_url}/wp-json/wp/v2/posts?per_page=1"
        
        try:
            response = await self.client.get(api_url)
            response.raise_for_status()
            
            # X-WP-Total 헤더에서 전체 개수 추출
            total_header = response.headers.get('X-WP-Total')
            if total_header:
                return int(total_header)
            
            logger.warning(f"X-WP-Total header not found for blog: {blog.name}")
            return 0
            
        except httpx.RequestError as e:
            logger.error(f"Request error getting total count for {blog.name}: {e}")
            return 0
        except Exception as e:
            logger.error(f"Error getting total count for blog {blog.name}: {e}")
            return 0
    
    async def _crawl_posts_page(self, blog: BlogEntity, offset: int) -> List[PostEntity]:
        """특정 페이지의 포스트들을 크롤링"""
        api_url = f"{blog.site_url}/wp-json/wp/v2/posts?per_page={self.posts_per_page}&offset={offset}"
        
        try:
            response = await self.client.get(api_url)
            response.raise_for_status()
            
            wp_posts = response.json()
            posts = []
            
            for wp_post in wp_posts:
                try:
                    post = self._create_post_from_wp_data(wp_post, blog)
                    if post:
                        posts.append(post)
                except Exception as e:
                    logger.error(f"Error processing WordPress post (ID: {wp_post.get('id', 'unknown')}): {e}")
                    continue
            
            logger.debug(f"Crawled {len(posts)} posts from offset {offset} for blog: {blog.name}")
            return posts
            
        except httpx.RequestError as e:
            logger.error(f"Request error crawling posts for {blog.name} (offset: {offset}): {e}")
            return []
        except Exception as e:
            logger.error(f"Error crawling posts for blog {blog.name} (offset: {offset}): {e}")
            return []
    
    def _create_post_from_wp_data(self, wp_post: Dict[str, Any], blog: BlogEntity) -> Optional[PostEntity]:
        """WordPress 포스트 데이터를 PostEntity로 변환"""
        try:
            # 제목 추출 및 정리
            title_data = wp_post.get('title', {})
            title = self._clean_html(title_data.get('rendered', '')) if isinstance(title_data, dict) else str(title_data)
            
            if not title.strip():
                logger.warning(f"Skipping post with empty title (ID: {wp_post.get('id', 'unknown')})")
                return None
            
            # 콘텐츠 추출 및 정리
            content_data = wp_post.get('content', {})
            content = self._clean_html(content_data.get('rendered', '')) if isinstance(content_data, dict) else str(content_data)
            
            # 요약 추출
            excerpt_data = wp_post.get('excerpt', {})
            excerpt = self._clean_html(excerpt_data.get('rendered', '')) if isinstance(excerpt_data, dict) else str(excerpt_data)
            
            # URL
            original_url = wp_post.get('link', '')
            if not original_url:
                logger.warning(f"Skipping post with empty URL (ID: {wp_post.get('id', 'unknown')})")
                return None
            
            # 작성자 (ID로 제공되는 경우가 많음)
            author = str(wp_post.get('author', 'Unknown'))
            
            # 발행일
            published_at = self._extract_published_date(wp_post.get('date'))
            
            # 콘텐츠 길이 제한
            if len(content) > 2000:
                content = content[:2000] + "..."
            
            if len(excerpt) > 500:
                excerpt = excerpt[:500] + "..."
            
            return PostEntity(
                title=title.strip(),
                content=content.strip() if content else excerpt.strip(),
                original_url=original_url,
                author=author,
                published_at=published_at,
                blog_id=blog.id
            )
            
        except Exception as e:
            logger.error(f"Error creating post from WordPress data: {e}")
            return None
    
    def _clean_html(self, html_content: str) -> str:
        """HTML 태그를 제거하고 텍스트를 정리"""
        if not html_content:
            return ""
        
        # HTML 태그 제거
        clean_text = re.sub(r'<[^>]*>', '', html_content)
        
        # HTML 엔티티 디코딩 (기본적인 것들)
        clean_text = clean_text.replace('&amp;', '&')
        clean_text = clean_text.replace('&lt;', '<')
        clean_text = clean_text.replace('&gt;', '>')
        clean_text = clean_text.replace('&quot;', '"')
        clean_text = clean_text.replace('&#8217;', "'")
        clean_text = clean_text.replace('&#8230;', "...")
        clean_text = clean_text.replace('&nbsp;', ' ')
        
        # 연속된 공백 정리
        clean_text = re.sub(r'\s+', ' ', clean_text)
        
        return clean_text.strip()
    
    def _extract_published_date(self, date_str: Optional[str]) -> Optional[datetime]:
        """발행일 문자열을 datetime 객체로 변환"""
        if not date_str:
            return datetime.now(timezone.utc)
        
        try:
            # WordPress는 ISO 형식으로 날짜를 제공
            return date_parser.parse(date_str)
        except (ValueError, TypeError) as e:
            logger.warning(f"Error parsing date '{date_str}': {e}")
            return datetime.now(timezone.utc)
    
    async def backfill_and_save_to_db(self, blog: BlogEntity) -> Dict[str, int]:
        """WordPress 블로그를 백필링하고 DB에 저장"""
        logger.info(f"Starting backfill and save for blog: {blog.name}")
        
        # 크롤링 실행
        posts = await self.backfill_all_posts(blog)
        
        # DB에 저장
        result = self.save_posts_to_db(posts, blog.id)
        
        logger.info(f"Backfill completed for blog: {blog.name}. "
                   f"Crawled: {len(posts)}, Saved: {result['saved']}, "
                   f"Skipped: {result['skipped']}, Errors: {result['errors']}")
        
        return result
    
    def save_posts_to_db(self, posts: List[PostEntity], blog_id: int) -> Dict[str, int]:
        """포스트 목록을 DB에 저장"""
        db = SessionLocal()
        saved_count = 0
        skipped_count = 0
        error_count = 0
        
        try:
            for post in posts:
                try:
                    # 중복 체크 (original_url 기준)
                    existing_post = db.query(PostEntity).filter(
                        PostEntity.original_url == post.original_url
                    ).first()
                    
                    if existing_post:
                        logger.debug(f"Post already exists, skipping: {post.title}")
                        skipped_count += 1
                        continue
                    
                    # 새 포스트 생성
                    new_post = PostEntity(
                        title=post.title,
                        content=post.content,
                        original_url=post.original_url,
                        author=post.author,
                        published_at=post.published_at,
                        blog_id=blog_id
                    )
                    
                    db.add(new_post)
                    saved_count += 1
                    
                except IntegrityError as e:
                    logger.warning(f"Integrity error saving post '{post.title}': {e}")
                    db.rollback()
                    skipped_count += 1
                except Exception as e:
                    logger.error(f"Error saving post '{post.title}': {e}")
                    db.rollback()
                    error_count += 1
            
            # 모든 변경사항 커밋
            db.commit()
            logger.info(f"Successfully saved {saved_count} posts to database")
            
        except Exception as e:
            logger.error(f"Database error during bulk save: {e}")
            db.rollback()
            
        finally:
            db.close()
        
        return {
            "saved": saved_count,
            "skipped": skipped_count, 
            "errors": error_count,
            "total": len(posts)
        }
    
    def get_blog_from_db(self, blog_name: str) -> Optional[BlogEntity]:
        """DB에서 블로그 정보 조회"""
        db = SessionLocal()
        try:
            blog = db.query(BlogEntity).filter(BlogEntity.name == blog_name).first()
            if blog:
                # Detach from session so it can be used after session closes
                db.expunge(blog)
            return blog
        except Exception as e:
            logger.error(f"Error getting blog from DB: {e}")
            return None
        finally:
            db.close()

    async def close(self):
        """HTTP 클라이언트 종료"""
        await self.client.aclose()