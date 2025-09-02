import httpx
import asyncio
from datetime import datetime
from typing import List, Optional, Dict, Any
from loguru import logger
from sqlalchemy.exc import IntegrityError

from ...models import BlogEntity, PostEntity
from ...core.config import settings
from ...core.database import SessionLocal
from .base_crawler import AsyncHTTPCrawler


class TossCrawler(AsyncHTTPCrawler):
    """토스 기술블로그 API 기반 크롤러"""
    
    def __init__(self):
        self.client = httpx.AsyncClient(
            headers={"User-Agent": settings.crawler_user_agent},
            timeout=settings.crawler_timeout
        )
        self.request_delay = 1.0
        self.base_url = "https://toss.tech"
        self.api_url = "https://api-public.toss.im/api-public/v3/ipd-thor/api/v1/workspaces/15/posts"

    async def backfill_all_posts(self, blog: BlogEntity) -> List[PostEntity]:
        """토스 기술블로그의 모든 포스트를 백필링"""
        logger.info(f"Starting Toss backfill for blog: {blog.name}")
        
        try:
            all_posts = []
            page = 1
            
            while True:
                # API 호출하여 페이지 데이터 가져오기
                posts_data = await self._fetch_posts_page(page)
                
                if not posts_data or not posts_data.get("success", {}).get("results"):
                    logger.info(f"No more posts to fetch at page {page}")
                    break
                
                # 현재 페이지에서 포스트 추출
                page_posts = self._extract_posts_from_page(posts_data, blog)
                if page_posts:
                    all_posts.extend(page_posts)
                    logger.info(f"Fetched {len(page_posts)} posts from page {page}")
                
                # 다음 페이지로 이동할 수 있는지 확인
                if not posts_data.get("success", {}).get("next"):
                    logger.info("Reached the last page")
                    break
                
                page += 1
                
                # 요청 간 지연
                await asyncio.sleep(self.request_delay)
            
            logger.info(f"Toss backfill completed. Total posts: {len(all_posts)}")
            return all_posts
            
        except Exception as e:
            logger.error(f"Error in Toss backfill: {str(e)}")
            raise
        finally:
            await self.close()

    async def _fetch_posts_page(self, page: int) -> Optional[Dict[str, Any]]:
        """API에서 페이지 데이터 가져오기"""
        params = {"page": page}
        
        try:
            # 요청 URL 로깅
            request_url = f"{self.api_url}?page={page}"
            logger.info(f"Fetching Toss posts from: {request_url}")
            
            response = await self.client.get(self.api_url, params=params)
            response.raise_for_status()
            
            return response.json()
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error fetching Toss posts page {page}: {e.response.status_code}")
            return None
        except Exception as e:
            logger.error(f"Error fetching Toss posts page {page}: {str(e)}")
            return None

    def _extract_posts_from_page(self, posts_data: Dict[str, Any], blog: BlogEntity) -> List[PostEntity]:
        """페이지 데이터에서 포스트 엔티티 리스트 추출"""
        posts = []
        results = posts_data.get("success", {}).get("results", [])
        
        for post_data in results:
            try:
                post = self._create_post_entity(post_data, blog)
                if post:
                    posts.append(post)
            except Exception as e:
                logger.warning(f"Error creating post entity: {str(e)}")
                continue
        
        return posts

    def _create_post_entity(self, post_data: Dict[str, Any], blog: BlogEntity) -> Optional[PostEntity]:
        """API 응답 데이터에서 PostEntity 생성"""
        try:
            post_id = post_data.get("id")
            if not post_id:
                return None
            
            # 필수 필드 검증
            title = post_data.get("title")
            if not title:
                return None
            
            # 작성자 처리
            author = None
            editor = post_data.get("editor", {})
            if editor and isinstance(editor, dict):
                author = editor.get("name")
            
            # 발행일 처리
            published_at = None
            published_time = post_data.get("publishedTime")
            if published_time:
                try:
                    # ISO 8601 형식 파싱 (Korean timezone +09:00)
                    published_at = datetime.fromisoformat(published_time.replace("Z", "+00:00"))
                except ValueError:
                    logger.warning(f"Failed to parse date: {published_time}")
            
            # 콘텐츠 처리
            content = (
                post_data.get("shortDescription")
                or post_data.get("seoConfig", {}).get("description")
                or post_data.get("openGraph", {}).get("description")
                or ""
            )
        
            # URL 생성 (slug 우선, 없으면 ID)
            url_slug = post_data.get('seoConfig', {}).get('urlSlug')
            if url_slug:
                original_url = f"https://toss.tech/article/{url_slug}"
            else:
                original_url = f"https://toss.tech/article/{post_id}"
            
            return PostEntity(
                title=title,
                content=content,
                original_url=original_url,
                author=author,
                published_at=published_at,
                blog_id=blog.id
            )
            
        except Exception as e:
            logger.error(f"Error creating PostEntity: {str(e)}")
            return None

    async def backfill_and_save_to_db(self, blog: BlogEntity) -> Dict[str, int]:
        """토스 블로그를 백필링하고 DB에 저장"""
        logger.info(f"Starting Toss backfill and save for blog: {blog.name}")
        
        # 크롤링 실행
        posts = await self.backfill_all_posts(blog)
        
        # DB에 저장
        result = self.save_posts_to_db(posts, blog.id)
        
        logger.info(f"Toss backfill completed for blog: {blog.name}. "
                   f"Crawled: {len(posts)}, Saved: {result['saved']}, "
                   f"Skipped: {result['skipped']}, Errors: {result['errors']}")
        
        return result

    def save_posts_to_db(self, posts: List[PostEntity], blog_id: int) -> Dict[str, int]:
        """포스트들을 데이터베이스에 저장"""
        saved_count = 0
        skipped_count = 0
        error_count = 0
        
        with SessionLocal() as db:
            for post in posts:
                try:
                    # 중복 확인 (original_url 기준)
                    existing_post = db.query(PostEntity).filter(
                        PostEntity.original_url == post.original_url
                    ).first()
                    
                    if existing_post:
                        logger.debug(f"Post already exists: {post.original_url}")
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
                    db.commit()
                    db.refresh(new_post)
                    saved_count += 1
                    logger.debug(f"Saved post: {post.title}")
                    
                except IntegrityError as e:
                    db.rollback()
                    logger.warning(f"Integrity error saving post {post.original_url}: {str(e)}")
                    error_count += 1
                except Exception as e:
                    db.rollback()
                    logger.error(f"Error saving post {post.original_url}: {str(e)}")
                    error_count += 1
        
        result = {
            'total': len(posts),
            'saved': saved_count,
            'skipped': skipped_count,
            'errors': error_count
        }
        
        logger.info(f"Saved {saved_count} new posts to database (skipped: {skipped_count}, errors: {error_count})")
        return result