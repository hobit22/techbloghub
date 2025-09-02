import httpx
import asyncio
import json
import re
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any, Set
from loguru import logger
from sqlalchemy.exc import IntegrityError
from bs4 import BeautifulSoup

from ...models import BlogEntity, PostEntity
from ...core.config import settings
from ...core.database import SessionLocal


class MediumCrawler:
    """Medium 기반 기술블로그 범용 크롤러 (실제 Apollo State + GraphQL 기반)"""
    
    def __init__(self):
        self.client = httpx.AsyncClient(
            headers={
                "User-Agent": settings.crawler_user_agent,
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language": "en-US,en;q=0.5",
                "Accept-Encoding": "gzip, deflate",
                "Connection": "keep-alive",
                "Referer": "https://medium.com/"
            },
            timeout=httpx.Timeout(30.0),
            verify=False
        )
        self.request_delay = 1.0
        self.base_url = "https://medium.com"
        self.graphql_url = "https://medium.com/_/graphql"
        
        # GraphQL 요청용 별도 헤더
        self.graphql_headers = {
            "User-Agent": settings.crawler_user_agent,
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Referer": "https://medium.com/"
        }
    
    async def backfill_all_posts(self, blog: BlogEntity) -> List[PostEntity]:
        """Medium 기술블로그의 모든 포스트를 백필링 (새로운 방식)"""
        logger.info(f"Starting Medium backfill for blog: {blog.name}")
        
        try:
            # site_url에서 publication slug 추출 (예: musinsa-tech)
            publication_slug = self._extract_publication_slug(blog.site_url)
            if not publication_slug:
                logger.error(f"Could not extract publication slug from URL: {blog.site_url}")
                return []
            
            logger.info(f"Extracted publication slug: {publication_slug}")
            
            # 1단계: 모든 페이지에서 postIds 수집
            all_post_ids = await self._collect_all_post_ids(publication_slug)
            if not all_post_ids:
                logger.warning(f"No post IDs found for publication: {publication_slug}")
                return []
            
            logger.info(f"Found {len(all_post_ids)} unique post IDs")
            
            # 2단계: GraphQL로 상세 정보 수집
            all_posts = await self._fetch_posts_details(all_post_ids, blog)
            
            logger.info(f"Completed Medium backfill for blog: {blog.name}. Total posts: {len(all_posts)}")
            return all_posts
            
        except Exception as e:
            logger.error(f"Error during Medium backfill for blog: {blog.name}: {e}")
            return []
    
    def _extract_publication_slug(self, site_url: str) -> Optional[str]:
        """Medium URL에서 publication slug 추출"""
        try:
            # https://medium.com/musinsa-tech → musinsa-tech
            # medium.com/ 이후의 첫 번째 세그먼트가 publication slug
            pattern = r'medium\.com/([^/?#]+)'
            match = re.search(pattern, site_url)
            if match:
                return match.group(1)
            return None
        except Exception as e:
            logger.error(f"Error extracting publication slug from {site_url}: {e}")
            return None
    
    async def _collect_all_post_ids(self, publication_slug: str) -> Set[str]:
        """모든 페이지에서 post IDs 수집"""
        all_post_ids = set()
        
        try:
            # 1. 메인 페이지 크롤링
            main_url = f"{self.base_url}/{publication_slug}"
            main_post_ids = await self._extract_post_ids_from_page(main_url)
            all_post_ids.update(main_post_ids)
            logger.info(f"Found {len(main_post_ids)} post IDs from main page")
            
            # 2. 서브페이지 URL들 발견 및 크롤링
            subpage_urls = await self._discover_subpage_urls(main_url)
            logger.info(f"Discovered {len(subpage_urls)} subpages")
            
            for subpage_url in subpage_urls:
                try:
                    await asyncio.sleep(self.request_delay)  # Rate limiting
                    subpage_post_ids = await self._extract_post_ids_from_page(subpage_url)
                    all_post_ids.update(subpage_post_ids)
                    logger.info(f"Found {len(subpage_post_ids)} post IDs from subpage: {subpage_url}")
                except Exception as e:
                    logger.error(f"Error crawling subpage {subpage_url}: {e}")
                    continue
            
            return all_post_ids
            
        except Exception as e:
            logger.error(f"Error collecting post IDs for {publication_slug}: {e}")
            return set()
    
    async def _discover_subpage_urls(self, main_url: str) -> List[str]:
        """메인 페이지에서 서브페이지 URL들 발견"""
        try:
            response = await self.client.get(main_url)
            response.raise_for_status()
            
            soup = BeautifulSoup(response.text, 'html.parser')
            subpage_urls = set()
            
            # 여러 패턴으로 서브페이지 링크 찾기
            patterns = [
                # /publication-slug/subpage/hash 패턴
                r'/[^/]+/subpage/[a-f0-9]{12}',
                # navigation 링크 패턴
                r'/[^/]+/tagged/[^/?#]+'
            ]
            
            # 네비게이션 링크 찾기
            nav_links = soup.find_all('a', href=True)
            
            for link in nav_links:
                href = link.get('href', '')
                
                # 패턴 매칭 및 URL 완성
                for pattern in patterns:
                    if re.search(pattern, href):
                        if href.startswith('/'):
                            full_url = f"{self.base_url}{href}"
                        elif href.startswith('http'):
                            full_url = href
                        else:
                            continue
                            
                        # 쌍리 매개변수 제거
                        if '?' in full_url:
                            full_url = full_url.split('?')[0]
                            
                        subpage_urls.add(full_url)
            
            return list(subpage_urls)
            
        except Exception as e:
            logger.error(f"Error discovering subpages from {main_url}: {e}")
            return []
    
    async def _extract_post_ids_from_page(self, page_url: str) -> Set[str]:
        """특정 페이지에서 Apollo State로부터 post IDs 추출"""
        max_retries = 3
        retry_delay = 2.0
        
        for attempt in range(max_retries):
            try:
                response = await self.client.get(page_url)
                response.raise_for_status()
                
                # Apollo State 추출 (더 정확한 JSON 파싱)
                apollo_state_str = self._extract_apollo_state_json(response.text)
                
                if not apollo_state_str:
                    logger.warning(f"No Apollo State found in page: {page_url} (attempt {attempt + 1})")
                    if attempt < max_retries - 1:
                        await asyncio.sleep(retry_delay)
                        continue
                    return set()
                
                # JSON 파싱 시도
                try:
                    apollo_state = json.loads(apollo_state_str)
                except json.JSONDecodeError as e:
                    logger.warning(f"JSON parsing failed for {page_url}: {e} (attempt {attempt + 1})")
                    if attempt < max_retries - 1:
                        await asyncio.sleep(retry_delay)
                        continue
                    return set()
                
                # "Post:xxxxx" 패턴으로 post ID 추출
                post_ids = set()
                for key in apollo_state.keys():
                    if key.startswith('Post:'):
                        post_id = key.replace('Post:', '')
                        # Medium post ID 검증 (10자리 이상의 헥스)
                        if post_id and len(post_id) >= 10 and re.match(r'^[a-f0-9]+$', post_id):
                            post_ids.add(post_id)
                
                logger.debug(f"Extracted {len(post_ids)} post IDs from {page_url}")
                return post_ids
                
            except httpx.HTTPStatusError as e:
                logger.warning(f"HTTP {e.response.status_code} error for {page_url} (attempt {attempt + 1}): {e}")
                if e.response.status_code == 429:  # Rate limit
                    await asyncio.sleep(retry_delay * 2)
                elif attempt < max_retries - 1:
                    await asyncio.sleep(retry_delay)
                else:
                    return set()
            except Exception as e:
                logger.error(f"Unexpected error extracting post IDs from {page_url} (attempt {attempt + 1}): {e}")
                if attempt < max_retries - 1:
                    await asyncio.sleep(retry_delay)
                else:
                    return set()
        
        return set()
    
    def _extract_apollo_state_json(self, html_text: str) -> Optional[str]:
        """HTML에서 완전한 Apollo State JSON 추출"""
        try:
            # window.__APOLLO_STATE__ = 패턴 찾기
            apollo_start_pattern = r'window\.__APOLLO_STATE__\s*=\s*'
            match = re.search(apollo_start_pattern, html_text)
            
            if not match:
                return None
            
            # JSON 시작 위치
            start_pos = match.end()
            
            # 중괄호 카운팅으로 완전한 JSON 추출
            brace_count = 0
            json_start = None
            json_end = None
            
            for i, char in enumerate(html_text[start_pos:], start_pos):
                if char == '{':
                    if json_start is None:
                        json_start = i
                    brace_count += 1
                elif char == '}':
                    brace_count -= 1
                    if brace_count == 0 and json_start is not None:
                        json_end = i + 1
                        break
            
            if json_start is not None and json_end is not None:
                return html_text[json_start:json_end]
            
            return None
            
        except Exception as e:
            logger.debug(f"Error extracting Apollo State JSON: {e}")
            return None
    
    async def backfill_and_save_to_db(self, blog: BlogEntity) -> Dict[str, int]:
        """Medium 블로그를 백필링하고 DB에 저장"""
        logger.info(f"Starting Medium backfill and save for blog: {blog.name}")
        
        # 크롤링 실행
        posts = await self.backfill_all_posts(blog)
        
        # DB에 저장
        result = self.save_posts_to_db(posts, blog.id)
        
        logger.info(f"Medium backfill completed for blog: {blog.name}. "
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
                        logger.debug(f"Medium post already exists, skipping: {post.title}")
                        skipped_count += 1
                        continue
                    
                    # 새 포스트 생성 (기본 필드만)
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
                    logger.warning(f"Integrity error saving Medium post '{post.title}': {e}")
                    db.rollback()
                    skipped_count += 1
                except Exception as e:
                    logger.error(f"Error saving Medium post '{post.title}': {e}")
                    db.rollback()
                    error_count += 1
            
            # 모든 변경사항 커밋
            db.commit()
            logger.info(f"Successfully saved {saved_count} Medium posts to database")
            
        except Exception as e:
            logger.error(f"Database error during Medium bulk save: {e}")
            db.rollback()
            
        finally:
            db.close()
        
        return {
            "saved": saved_count,
            "skipped": skipped_count, 
            "errors": error_count,
            "total": len(posts)
        }
    
    async def _fetch_posts_details(self, post_ids: Set[str], blog: BlogEntity) -> List[PostEntity]:
        """수집된 post IDs로 GraphQL 상세 정보 수집"""
        posts = []
        post_ids_list = list(post_ids)
        
        # 배치로 처리 (Medium은 한 번에 여러 postIds 요청 가능)
        batch_size = 25  # Medium의 일반적인 제한
        
        for i in range(0, len(post_ids_list), batch_size):
            batch_ids = post_ids_list[i:i + batch_size]
            
            try:
                batch_posts = await self._fetch_posts_batch(batch_ids, blog)
                posts.extend(batch_posts)
                
                # Rate limiting
                if i + batch_size < len(post_ids_list):
                    await asyncio.sleep(self.request_delay)
                    
            except Exception as e:
                logger.error(f"Error fetching batch {i//batch_size + 1}: {e}")
                continue
        
        return posts
    
    async def _fetch_posts_batch(self, post_ids: List[str], blog: BlogEntity) -> List[PostEntity]:
        """배치 단위로 GraphQL로 포스트 상세 정보 수집"""
        try:
            # 실제 PublicationSectionPostsQuery 구성
            query_payload = {
                "operationName": "PublicationSectionPostsQuery",
                "variables": {
                    "postIds": post_ids
                },
                "query": """
query PublicationSectionPostsQuery($postIds: [ID!]!) {
  postResults(postIds: $postIds) {
    ... on Post {
      id
      title
      uniqueSlug
      firstPublishedAt
      latestPublishedAt
      creator {
        id
        name
        username
      }
      extendedPreviewContent {
        subtitle
        isFullContent
      }
      previewImage {
        id
        alt
        focusPercentX
        focusPercentY
      }
      readingTime
      isLocked
      mediumUrl
      collection {
        id
        slug
        name
        domain
      }
      __typename
    }
    __typename
  }
}
                """
            }
            
            # GraphQL 요청 (배열로 래핑)
            headers = self.graphql_headers.copy()
            response = await self.client.post(
                self.graphql_url,
                json=[query_payload],
                headers=headers
            )
            response.raise_for_status()
            
            data = response.json()
            
            if not isinstance(data, list) or len(data) == 0:
                logger.warning("Invalid GraphQL response format")
                return []
            
            result_data = data[0].get('data', {})
            post_results = result_data.get('postResults', [])
            
            posts = []
            for post_data in post_results:
                if post_data.get('__typename') == 'Post':
                    post = self._create_post_from_graphql_data(post_data, blog)
                    if post:
                        posts.append(post)
            
            logger.debug(f"Processed {len(posts)} posts from GraphQL batch")
            return posts
            
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error in GraphQL request: {e.response.status_code} - {e.response.text}")
            return []
        except Exception as e:
            logger.error(f"Error in GraphQL batch request: {e}")
            return []
    
    def _create_post_from_graphql_data(self, post_data: Dict[str, Any], blog: BlogEntity) -> Optional[PostEntity]:
        """GraphQL 데이터로부터 PostEntity 생성"""
        try:
            # 기본 정보
            post_id = post_data.get('id')
            title = post_data.get('title', '').strip()
            if not title or not post_id:
                logger.warning(f"Skipping post with missing title or ID: {post_id}")
                return None
            
            # URL 생성
            unique_slug = post_data.get('uniqueSlug')
            medium_url = post_data.get('mediumUrl')
            
            collection = post_data.get('collection', {})
            if collection and collection.get('slug'):
                if collection.get('domain'):
                    original_url = f"https://{collection['domain']}/{unique_slug}"
                else:
                    original_url = f"https://medium.com/{collection['slug']}/{unique_slug}"
            else:
                original_url = medium_url or f"https://medium.com/p/{post_id}"
            
            # 콘텐츠 (확장된 미리보기 사용)
            extended_preview = post_data.get('extendedPreviewContent', {})
            subtitle = extended_preview.get('subtitle', '') if extended_preview else ''
            content = subtitle.strip() if subtitle else title
            
            # 발행일
            published_timestamp = post_data.get('firstPublishedAt') or post_data.get('latestPublishedAt')
            if published_timestamp:
                try:
                    published_at = datetime.fromtimestamp(published_timestamp / 1000, tz=timezone.utc)
                except Exception as e:
                    logger.warning(f"Error parsing timestamp '{published_timestamp}': {e}")
                    published_at = datetime.now(timezone.utc)
            else:
                published_at = datetime.now(timezone.utc)
            
            # 작성자 정보
            creator = post_data.get('creator', {})
            if creator:
                author = creator.get('name') or creator.get('username') or "Unknown"
            else:
                author = "Unknown"
            
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
            logger.error(f"Error creating post from GraphQL data: {e}")
            return None
    
    async def close(self):
        """HTTP 클라이언트 종료"""
        await self.client.aclose()