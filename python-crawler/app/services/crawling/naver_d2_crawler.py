import httpx
import asyncio
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any
from loguru import logger
import re
from sqlalchemy.exc import IntegrityError

from ...models import BlogEntity, PostEntity
from ...core.config import settings
from ...core.database import SessionLocal


class NaverD2Crawler:
    """네이버 D2 API를 사용한 크롤러"""
    
    def __init__(self):
        self.client = httpx.AsyncClient(
            headers={"User-Agent": settings.crawler_user_agent},
            timeout=httpx.Timeout(30.0),
            verify=False  # SSL 검증 비활성화 (필요시)
        )
        self.posts_per_page = 20
        self.request_delay = 0.5  # 네이버 서버 부하 고려하여 짧게 설정
        self.base_url = "https://d2.naver.com"
        self.api_url = "https://d2.naver.com/api/v1/contents"
    
    async def backfill_all_posts(self, blog: BlogEntity) -> List[PostEntity]:
        """네이버 D2의 모든 포스트를 백필링"""
        logger.info(f"Starting D2 backfill for blog: {blog.name}")
        
        try:
            # 전체 포스트 개수 조회
            total_count = await self._get_total_post_count()
            if total_count == 0:
                logger.warning(f"No posts found for D2 blog: {blog.name}")
                return []
            
            logger.info(f"Total posts to crawl: {total_count} for D2 blog: {blog.name}")
            
            # 페이지 수 계산
            total_pages = (total_count + self.posts_per_page - 1) // self.posts_per_page
            
            all_posts = []
            
            # 각 페이지를 순차적으로 크롤링
            for page in range(total_pages):
                page_num = page + 1  # D2 API는 1부터 시작
                logger.info(f"Crawling page {page_num}/{total_pages} for D2 blog: {blog.name}")
                
                try:
                    posts = await self._crawl_posts_page(blog, page_num)
                    all_posts.extend(posts)
                    
                    # Rate limiting
                    if page < total_pages - 1:
                        await asyncio.sleep(self.request_delay)
                        
                except Exception as e:
                    logger.error(f"Error crawling page {page_num} for D2 blog {blog.name}: {e}")
                    continue
            
            logger.info(f"Completed D2 backfill for blog: {blog.name}. Total posts: {len(all_posts)}")
            return all_posts
            
        except Exception as e:
            logger.error(f"Error during D2 backfill for blog: {blog.name}: {e}")
            return []
    
    async def _get_total_post_count(self) -> int:
        """전체 포스트 개수 조회"""
        try:
            response = await self.client.get(f"{self.api_url}?page=1&size=1")
            response.raise_for_status()
            
            data = response.json()
            total_elements = data.get('page', {}).get('totalElements', 0)
            
            logger.info(f"D2 total posts found: {total_elements}")
            return total_elements
            
        except httpx.RequestError as e:
            logger.error(f"Request error getting D2 total count: {e}")
            return 0
        except Exception as e:
            logger.error(f"Error getting D2 total count: {e}")
            return 0
    
    async def _crawl_posts_page(self, blog: BlogEntity, page: int) -> List[PostEntity]:
        """특정 페이지의 포스트들을 크롤링"""
        try:
            response = await self.client.get(
                f"{self.api_url}?page={page}&size={self.posts_per_page}"
            )
            response.raise_for_status()
            
            data = response.json()
            d2_posts = data.get('content', [])
            posts = []
            
            for d2_post in d2_posts:
                try:
                    post = self._create_post_from_d2_data(d2_post, blog)
                    if post:
                        posts.append(post)
                except Exception as e:
                    logger.error(f"Error processing D2 post (URL: {d2_post.get('url', 'unknown')}): {e}")
                    continue
            
            logger.debug(f"Crawled {len(posts)} posts from page {page} for D2 blog: {blog.name}")
            return posts
            
        except httpx.RequestError as e:
            logger.error(f"Request error crawling D2 posts for page {page}: {e}")
            return []
        except Exception as e:
            logger.error(f"Error crawling D2 posts for page {page}: {e}")
            return []
    
    def _create_post_from_d2_data(self, d2_post: Dict[str, Any], blog: BlogEntity) -> Optional[PostEntity]:
        """네이버 D2 포스트 데이터를 PostEntity로 변환"""
        try:
            # 제목
            title = d2_post.get('postTitle', '').strip()
            if not title:
                logger.warning(f"Skipping D2 post with empty title (URL: {d2_post.get('url', 'unknown')})")
                return None
            
            # URL - 절대 경로로 변환
            url_path = d2_post.get('url', '')
            if not url_path:
                logger.warning(f"Skipping D2 post with empty URL")
                return None
            
            original_url = f"{self.base_url}{url_path}"
            
            # 콘텐츠 - HTML을 정리
            content_html = d2_post.get('postHtml', '')
            content = self._clean_html(content_html)
            
            # 발행일 - 타임스탬프를 datetime으로 변환
            published_timestamp = d2_post.get('postPublishedAt')
            if published_timestamp:
                # 밀리초 타임스탬프를 초로 변환
                published_at = datetime.fromtimestamp(
                    published_timestamp / 1000, 
                    tz=timezone.utc
                )
            else:
                published_at = datetime.now(timezone.utc)
            
            # 작성자 - 현재는 ID만 있으므로 문자열로 저장
            author_data = d2_post.get('author', '')
            author = str(author_data) if author_data else 'Unknown'
            
            # 콘텐츠 길이 제한
            if len(content) > 2000:
                content = content[:2000] + "..."
            
            return PostEntity(
                title=title,
                content=content,
                original_url=original_url,
                author=author,
                published_at=published_at,
                blog_id=blog.id
            )
            
        except Exception as e:
            logger.error(f"Error creating post from D2 data: {e}")
            return None
    
    def _clean_html(self, html_content: str) -> str:
        """HTML 태그를 제거하고 텍스트를 정리"""
        if not html_content:
            return ""
        
        # HTML 태그 제거
        clean_text = re.sub(r'<[^>]*>', '', html_content)
        
        # HTML 엔티티 디코딩
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
    
    async def backfill_and_save_to_db(self, blog: BlogEntity) -> Dict[str, int]:
        """네이버 D2 블로그를 백필링하고 DB에 저장"""
        logger.info(f"Starting D2 backfill and save for blog: {blog.name}")
        
        # 크롤링 실행
        posts = await self.backfill_all_posts(blog)
        
        # DB에 저장
        result = self.save_posts_to_db(posts, blog.id)
        
        logger.info(f"D2 backfill completed for blog: {blog.name}. "
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
                        logger.debug(f"D2 post already exists, skipping: {post.title}")
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
                    logger.warning(f"Integrity error saving D2 post '{post.title}': {e}")
                    db.rollback()
                    skipped_count += 1
                except Exception as e:
                    logger.error(f"Error saving D2 post '{post.title}': {e}")
                    db.rollback()
                    error_count += 1
            
            # 모든 변경사항 커밋
            db.commit()
            logger.info(f"Successfully saved {saved_count} D2 posts to database")
            
        except Exception as e:
            logger.error(f"Database error during D2 bulk save: {e}")
            db.rollback()
            
        finally:
            db.close()
        
        return {
            "saved": saved_count,
            "skipped": skipped_count, 
            "errors": error_count,
            "total": len(posts)
        }
    
    async def close(self):
        """HTTP 클라이언트 종료"""
        await self.client.aclose()