import httpx
import asyncio
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any
from loguru import logger
from sqlalchemy.exc import IntegrityError
from dateutil import parser as date_parser

from ...models import BlogEntity, PostEntity
from ...core.config import settings
from ...core.database import SessionLocal


class NHNCloudCrawler:
    """NHN Cloud Meetup API를 사용한 크롤러"""
    
    def __init__(self):
        self.client = httpx.AsyncClient(
            headers={"User-Agent": settings.crawler_user_agent},
            timeout=httpx.Timeout(30.0),
            verify=False  # SSL 검증 비활성화 (필요시)
        )
        self.posts_per_page = 20
        self.request_delay = 0.5  # NHN 서버 부하 고려
        self.base_url = "https://meetup.nhncloud.com"
        self.api_url = "https://meetup.nhncloud.com/tcblog/v1.0/posts"
    
    async def backfill_all_posts(self, blog: BlogEntity) -> List[PostEntity]:
        """NHN Cloud Meetup의 모든 포스트를 백필링"""
        logger.info(f"Starting NHN Cloud backfill for blog: {blog.name}")
        
        try:
            # 전체 포스트 개수 조회
            total_count = await self._get_total_post_count()
            if total_count == 0:
                logger.warning(f"No posts found for NHN Cloud blog: {blog.name}")
                return []
            
            logger.info(f"Total posts to crawl: {total_count} for NHN Cloud blog: {blog.name}")
            
            # 페이지 수 계산
            total_pages = (total_count + self.posts_per_page - 1) // self.posts_per_page
            
            all_posts = []
            
            # 각 페이지를 순차적으로 크롤링
            for page in range(total_pages):
                page_num = page + 1  # NHN Cloud API는 1부터 시작
                logger.info(f"Crawling page {page_num}/{total_pages} for NHN Cloud blog: {blog.name}")
                
                try:
                    posts = await self._crawl_posts_page(blog, page_num)
                    all_posts.extend(posts)
                    
                    # Rate limiting
                    if page < total_pages - 1:
                        await asyncio.sleep(self.request_delay)
                        
                except Exception as e:
                    logger.error(f"Error crawling page {page_num} for NHN Cloud blog {blog.name}: {e}")
                    continue
            
            logger.info(f"Completed NHN Cloud backfill for blog: {blog.name}. Total posts: {len(all_posts)}")
            return all_posts
            
        except Exception as e:
            logger.error(f"Error during NHN Cloud backfill for blog: {blog.name}: {e}")
            return []
    
    async def _get_total_post_count(self) -> int:
        """전체 포스트 개수 조회"""
        try:
            response = await self.client.get(f"{self.api_url}?pageNo=1&rowsPerPage=1")
            response.raise_for_status()
            
            data = response.json()
            
            # API 응답 구조 확인
            if data.get('header', {}).get('isSuccessful') != True:
                logger.error(f"NHN Cloud API error: {data.get('header', {}).get('resultMessage', 'Unknown error')}")
                return 0
            
            total_count = data.get('totalCount', 0)
            
            logger.info(f"NHN Cloud total posts found: {total_count}")
            return total_count
            
        except httpx.RequestError as e:
            logger.error(f"Request error getting NHN Cloud total count: {e}")
            return 0
        except Exception as e:
            logger.error(f"Error getting NHN Cloud total count: {e}")
            return 0
    
    async def _crawl_posts_page(self, blog: BlogEntity, page: int) -> List[PostEntity]:
        """특정 페이지의 포스트들을 크롤링"""
        try:
            response = await self.client.get(
                f"{self.api_url}?pageNo={page}&rowsPerPage={self.posts_per_page}"
            )
            response.raise_for_status()
            
            data = response.json()
            
            # API 응답 성공 여부 확인
            if data.get('header', {}).get('isSuccessful') != True:
                logger.error(f"NHN Cloud API error on page {page}: {data.get('header', {}).get('resultMessage', 'Unknown error')}")
                return []
            
            nhn_posts = data.get('posts', [])
            posts = []
            
            for nhn_post in nhn_posts:
                try:
                    post = self._create_post_from_nhn_data(nhn_post, blog)
                    if post:
                        posts.append(post)
                except Exception as e:
                    logger.error(f"Error processing NHN Cloud post (ID: {nhn_post.get('postId', 'unknown')}): {e}")
                    continue
            
            logger.debug(f"Crawled {len(posts)} posts from page {page} for NHN Cloud blog: {blog.name}")
            return posts
            
        except httpx.RequestError as e:
            logger.error(f"Request error crawling NHN Cloud posts for page {page}: {e}")
            return []
        except Exception as e:
            logger.error(f"Error crawling NHN Cloud posts for page {page}: {e}")
            return []
    
    def _create_post_from_nhn_data(self, nhn_post: Dict[str, Any], blog: BlogEntity) -> Optional[PostEntity]:
        """NHN Cloud 포스트 데이터를 PostEntity로 변환"""
        try:
            # postPerLang에서 다국어 정보 추출 (한국어 우선)
            post_per_lang = nhn_post.get('postPerLang', {})
            
            # 제목
            title = post_per_lang.get('title', '').strip()
            if not title:
                logger.warning(f"Skipping NHN Cloud post with empty title (ID: {nhn_post.get('postId', 'unknown')})")
                return None
            
            content = nhn_post.get('contentPreview', title)
            
            # URL 생성 - postId를 기준으로 생성 (실제 URL 패턴 확인 필요)
            post_id = nhn_post.get('postId')
            if not post_id:
                logger.warning(f"Skipping NHN Cloud post with no postId")
                return None
            
            # NHN Cloud Meetup의 실제 URL 패턴을 추정
            original_url = f"{self.base_url}/posts/{post_id}"
            
            # 발행일 - publishTime 또는 regTime 사용
            publish_time = nhn_post.get('publishTime') or nhn_post.get('regTime')
            if publish_time:
                try:
                    # ISO 형식 날짜 파싱
                    published_at = date_parser.parse(publish_time)
                    # timezone naive인 경우 UTC로 가정
                    if published_at.tzinfo is None:
                        published_at = published_at.replace(tzinfo=timezone.utc)
                except Exception as e:
                    logger.warning(f"Error parsing NHN Cloud date '{publish_time}': {e}")
                    published_at = datetime.now(timezone.utc)
            else:
                published_at = datetime.now(timezone.utc)
            
            # 작성자 - regId에서 이메일 추출 (마스킹되어 있음)
            author_id = nhn_post.get('regId', 'Unknown')
            # 이메일에서 사용자명 추출 시도
            if '@' in author_id and '*' in author_id:
                # sunyo***@nhn.com -> sunyo*** 형태로 변경
                author = author_id.split('@')[0]
            else:
                author = str(author_id)
            
            # 태그 정보 추출
            tags = post_per_lang.get('tag', '')
            if tags and content == title:
                # 콘텐츠가 빈 경우 태그라도 추가
                content = f"{title}\n\n태그: {tags}"
            
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
            logger.error(f"Error creating post from NHN Cloud data: {e}")
            return None
    
    async def backfill_and_save_to_db(self, blog: BlogEntity) -> Dict[str, int]:
        """NHN Cloud 블로그를 백필링하고 DB에 저장"""
        logger.info(f"Starting NHN Cloud backfill and save for blog: {blog.name}")
        
        # 크롤링 실행
        posts = await self.backfill_all_posts(blog)
        
        # DB에 저장
        result = self.save_posts_to_db(posts, blog.id)
        
        logger.info(f"NHN Cloud backfill completed for blog: {blog.name}. "
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
                        logger.debug(f"NHN Cloud post already exists, skipping: {post.title}")
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
                    logger.warning(f"Integrity error saving NHN Cloud post '{post.title}': {e}")
                    db.rollback()
                    skipped_count += 1
                except Exception as e:
                    logger.error(f"Error saving NHN Cloud post '{post.title}': {e}")
                    db.rollback()
                    error_count += 1
            
            # 모든 변경사항 커밋
            db.commit()
            logger.info(f"Successfully saved {saved_count} NHN Cloud posts to database")
            
        except Exception as e:
            logger.error(f"Database error during NHN Cloud bulk save: {e}")
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