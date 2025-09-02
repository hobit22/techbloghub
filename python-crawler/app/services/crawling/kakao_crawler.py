import httpx
import asyncio
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any
from loguru import logger
from sqlalchemy.exc import IntegrityError

from ...models import BlogEntity, PostEntity
from ...core.config import settings
from ...core.database import SessionLocal
from .base_crawler import AsyncHTTPCrawler


class KakaoCrawler(AsyncHTTPCrawler):
    """카카오 기술블로그 API 기반 크롤러"""
    
    def __init__(self):
        self.client = httpx.AsyncClient(
            headers={
                "User-Agent": settings.crawler_user_agent,
                "Accept": "application/json",
                "Accept-Language": "en-US,en;q=0.5",
                "Accept-Encoding": "gzip, deflate",
                "Connection": "keep-alive",
                "Referer": "https://tech.kakao.com/"
            },
            timeout=httpx.Timeout(30.0),
            verify=False
        )
        self.request_delay = 1.0
        self.base_url = "https://tech.kakao.com"
        self.api_url = "https://tech.kakao.com/api/v1/posts/no-offset"

    async def backfill_all_posts(self, blog: BlogEntity) -> List[PostEntity]:
        """카카오 기술블로그의 모든 포스트를 백필링"""
        logger.info(f"Starting Kakao backfill for blog: {blog.name}")
        
        try:
            all_posts = []
            
            # 첫 번째 블록부터 시작
            first_seq = 0
            last_seq = 0
            first_page_number = 1
            last_page_number = 0
            block_direction = "NEXT"
            
            while True:
                # API 호출하여 포스트 블록 가져오기
                posts_data = await self._fetch_posts_block(
                    first_seq, last_seq, first_page_number, last_page_number, block_direction
                )
                
                if not posts_data or not posts_data.get("pages"):
                    logger.info("No more posts to fetch")
                    break
                
                # 현재 블록의 모든 페이지에서 포스트 추출
                block_posts = self._extract_posts_from_block(posts_data, blog)
                if block_posts:
                    all_posts.extend(block_posts)
                    logger.info(f"Fetched {len(block_posts)} posts from current block")
                
                # 다음 블록으로 이동할 수 있는지 확인
                if not posts_data.get("nextBlock", False):
                    logger.info("Reached the end of all blocks")
                    break
                
                # 다음 블록을 위한 파라미터 업데이트
                first_seq = posts_data.get("firstSeq", 0)
                last_seq = posts_data.get("lastSeq", 0)
                first_page_number = posts_data.get("firstPageNumber", 1)
                last_page_number = posts_data.get("lastPageNumber", 0)
                
                # 요청 간 지연
                await asyncio.sleep(self.request_delay)
            
            logger.info(f"Kakao backfill completed. Total posts: {len(all_posts)}")
            return all_posts
            
        except Exception as e:
            logger.error(f"Error in Kakao backfill: {str(e)}")
            raise
        finally:
            await self.close()

    async def _fetch_posts_block(self, first_seq: int, last_seq: int, 
                                first_page_number: int, last_page_number: int, 
                                block_direction: str) -> Optional[Dict[str, Any]]:
        """API에서 포스트 블록 데이터 가져오기"""
        logger.info(f"Fetching posts block: {first_seq}, {last_seq}, {first_page_number}, {last_page_number}, {block_direction}")
        params = {
            "categoryCode": "blog",
            "firstSeq": first_seq,
            "lastSeq": last_seq,
            "firstPageNumber": first_page_number,
            "lastPageNumber": last_page_number,
            "blockDirection": block_direction
        }
        
        try:
            response = await self.client.get(self.api_url, params=params)
            response.raise_for_status()
            return response.json()
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error fetching Kakao posts: {e.response.status_code}")
            return None
        except Exception as e:
            logger.error(f"Error fetching Kakao posts: {str(e)}")
            return None

    def _extract_posts_from_block(self, posts_data: Dict[str, Any], blog: BlogEntity) -> List[PostEntity]:
        """블록 데이터에서 포스트 엔티티 리스트 추출"""
        posts = []
        pages = posts_data.get("pages", [])
        
        for page in pages:
            contents = page.get("contents", [])
            for post_data in contents:
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
            
            # 작성자 처리 (단일 작성자 또는 첫 번째 작성자)
            author = None
            if "author" in post_data and post_data["author"]:
                author = post_data["author"].get("name")
            elif "authors" in post_data and post_data["authors"]:
                # 여러 작성자인 경우 첫 번째 작성자
                first_author = post_data["authors"][0]
                if isinstance(first_author, dict):
                    author = first_author.get("name")
                else:
                    author = str(first_author)
            
            # 발행일 처리
            published_at = None
            release_date_time = post_data.get("releaseDateTime")
            if release_date_time:
                try:
                    # 카카오 API 날짜 형식: "2025.09.02 09:25:00"
                    published_at = datetime.strptime(release_date_time, "%Y.%m.%d %H:%M:%S")
                    # UTC로 설정
                    published_at = published_at.replace(tzinfo=timezone.utc)
                except ValueError:
                    # 날짜 파싱 실패시 None으로 유지
                    logger.warning(f"Failed to parse date: {release_date_time}")
            
            # URL 생성
            original_url = f"https://tech.kakao.com/posts/{post_id}"
            
            return PostEntity(
                title=title,
                content=None,  # API에서 본문 제공하지 않음
                original_url=original_url,
                author=author,
                published_at=published_at,
                blog_id=blog.id
            )
            
        except Exception as e:
            logger.error(f"Error creating PostEntity: {str(e)}")
            return None

    async def backfill_and_save_to_db(self, blog: BlogEntity) -> Dict[str, int]:
        """카카오 블로그를 백필링하고 DB에 저장"""
        logger.info(f"Starting Kakao backfill and save for blog: {blog.name}")
        
        # 크롤링 실행
        posts = await self.backfill_all_posts(blog)
        
        # DB에 저장
        result = self.save_posts_to_db(posts, blog.id)
        
        logger.info(f"Kakao backfill completed for blog: {blog.name}. "
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