import httpx
import asyncio
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any
from loguru import logger
from dateutil import parser as date_parser
from sqlalchemy.exc import IntegrityError

from ...models import BlogEntity, PostEntity
from ...core.config import settings
from ...core.database import SessionLocal


class LYCorpCrawler:
    """LY Corporation (LINE Yahoo) 기술블로그 Gatsby page-data.json API를 사용한 크롤러"""
    
    def __init__(self):
        self.client = httpx.AsyncClient(
            headers={"User-Agent": settings.crawler_user_agent},
            timeout=httpx.Timeout(30.0),
            verify=False  # SSL 검증 비활성화 (필요시)
        )
        self.posts_per_page = 12  # LY Corp 블로그는 페이지당 12개 포스트
        self.request_delay = 0.5  # 서버 부하 고려
        self.base_url = "https://techblog.lycorp.co.jp"
        self.api_url = "https://techblog.lycorp.co.jp/page-data/ko/page"
    
    async def backfill_all_posts(self, blog: BlogEntity) -> List[PostEntity]:
        """LY Corporation 기술블로그의 모든 포스트를 백필링"""
        logger.info(f"Starting LY Corp backfill for blog: {blog.name}")
        
        try:
            all_posts = []
            page = 1
            
            # 페이지가 존재하는 동안 계속 크롤링
            while True:
                logger.info(f"Crawling page {page} for LY Corp blog: {blog.name}")
                
                try:
                    posts = await self._crawl_posts_page(blog, page)
                    
                    if not posts:  # 더 이상 포스트가 없으면 종료
                        logger.info(f"No more posts found at page {page}, stopping crawl")
                        break
                    
                    all_posts.extend(posts)
                    logger.info(f"Found {len(posts)} posts on page {page}")
                    
                    # Rate limiting
                    await asyncio.sleep(self.request_delay)
                    page += 1
                    
                except Exception as e:
                    logger.error(f"Error crawling page {page} for LY Corp blog {blog.name}: {e}")
                    break
            
            logger.info(f"Completed LY Corp backfill for blog: {blog.name}. Total posts: {len(all_posts)}")
            return all_posts
            
        except Exception as e:
            logger.error(f"Error during LY Corp backfill for blog: {blog.name}: {e}")
            return []
    
    async def _crawl_posts_page(self, blog: BlogEntity, page: int) -> List[PostEntity]:
        """특정 페이지의 포스트들을 크롤링"""
        try:
            response = await self.client.get(f"{self.api_url}/{page}/page-data.json")
            response.raise_for_status()
            
            data = response.json()
            
            # Gatsby page-data 구조에서 블로그 포스트 추출
            blog_query = data.get('result', {}).get('data', {}).get('BlogsQuery', {})
            lycorp_posts = blog_query.get('edges', [])
            
            if not lycorp_posts:
                logger.info(f"No posts found on page {page}")
                return []
            
            posts = []
            
            for lycorp_post_edge in lycorp_posts:
                try:
                    lycorp_post = lycorp_post_edge.get('node', {})
                    post = self._create_post_from_lycorp_data(lycorp_post, blog)
                    if post:
                        posts.append(post)
                except Exception as e:
                    logger.error(f"Error processing LY Corp post (ID: {lycorp_post.get('postId', 'unknown')}): {e}")
                    continue
            
            logger.debug(f"Crawled {len(posts)} posts from page {page} for LY Corp blog: {blog.name}")
            return posts
            
        except httpx.HTTPStatusError as e:
            if e.response.status_code == 404:
                logger.info(f"Page {page} not found (404), likely reached end of posts")
                return []
            else:
                logger.error(f"HTTP error crawling LY Corp posts for page {page}: {e}")
                return []
        except httpx.RequestError as e:
            logger.error(f"Request error crawling LY Corp posts for page {page}: {e}")
            return []
        except Exception as e:
            logger.error(f"Error crawling LY Corp posts for page {page}: {e}")
            return []
    
    def _create_post_from_lycorp_data(self, lycorp_post: Dict[str, Any], blog: BlogEntity) -> Optional[PostEntity]:
        """LY Corp 포스트 데이터를 PostEntity로 변환"""
        try:
            # 제목
            title = lycorp_post.get('title', '').strip()
            if not title:
                logger.warning(f"Skipping LY Corp post with empty title (ID: {lycorp_post.get('postId', 'unknown')})")
                return None
            
            # URL 생성 - slug 기반
            slug = lycorp_post.get('slug', '')
            if not slug:
                logger.warning(f"Skipping LY Corp post with no slug (ID: {lycorp_post.get('postId', 'unknown')})")
                return None
            
            original_url = f"{self.base_url}/ko/blog/{slug}/"
            
            # 콘텐츠 - 제목만 사용 (전체 내용은 별도 페이지 호출이 필요)
            content = title
            
            # 발행일 - pubdate 사용
            pubdate = lycorp_post.get('pubdate')
            if pubdate:
                try:
                    # ISO 형식 날짜 파싱
                    published_at = date_parser.parse(pubdate)
                    # timezone naive인 경우 UTC로 가정
                    if published_at.tzinfo is None:
                        published_at = published_at.replace(tzinfo=timezone.utc)
                except Exception as e:
                    logger.warning(f"Error parsing LY Corp date '{pubdate}': {e}")
                    published_at = datetime.now(timezone.utc)
            else:
                published_at = datetime.now(timezone.utc)
            
            # 작성자 - 현재 API에서는 작성자 정보가 명시적으로 없음
            author = "LY Corporation"
            
            # 태그 정보가 있다면 콘텐츠에 추가
            tags = lycorp_post.get('tags', [])
            if tags and isinstance(tags, list):
                tag_names = [tag.get('name', '') for tag in tags if isinstance(tag, dict)]
                tag_names = [name for name in tag_names if name]  # 빈 문자열 제거
                if tag_names:
                    content = f"{title}\n\n태그: {', '.join(tag_names)}"
            
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
            logger.error(f"Error creating post from LY Corp data: {e}")
            return None
    
    async def backfill_and_save_to_db(self, blog: BlogEntity) -> Dict[str, int]:
        """LY Corp 블로그를 백필링하고 DB에 저장"""
        logger.info(f"Starting LY Corp backfill and save for blog: {blog.name}")
        
        # 크롤링 실행
        posts = await self.backfill_all_posts(blog)
        
        # DB에 저장
        result = self.save_posts_to_db(posts, blog.id)
        
        logger.info(f"LY Corp backfill completed for blog: {blog.name}. "
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
                        logger.debug(f"LY Corp post already exists, skipping: {post.title}")
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
                    logger.warning(f"Integrity error saving LY Corp post '{post.title}': {e}")
                    db.rollback()
                    skipped_count += 1
                except Exception as e:
                    logger.error(f"Error saving LY Corp post '{post.title}': {e}")
                    db.rollback()
                    error_count += 1
            
            # 모든 변경사항 커밋
            db.commit()
            logger.info(f"Successfully saved {saved_count} LY Corp posts to database")
            
        except Exception as e:
            logger.error(f"Database error during LY Corp bulk save: {e}")
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