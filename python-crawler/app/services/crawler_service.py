from typing import List, Optional
from sqlalchemy.orm import Session
from loguru import logger

from ..models import BlogEntity, PostEntity, BlogStatus
from ..services.rss_crawler import RSSCrawler
from ..services.tag_classifier import tag_classifier
from ..services.elasticsearch_indexer import elasticsearch_indexer


class CrawlerService:
    """크롤링 관련 비즈니스 로직을 처리하는 서비스"""
    
    def __init__(self, db: Session):
        self.db = db
        self.rss_crawler = RSSCrawler()
        self.enable_tagging = True  # 태깅 활성화 여부
        self.enable_indexing = True  # ElasticSearch 인덱싱 활성화 여부
    
    async def crawl_all_active_blogs(self) -> dict:
        """모든 활성화된 블로그를 크롤링"""
        logger.info("Starting crawl for all active blogs")
        
        try:
            # 활성화된 블로그 목록 조회
            active_blogs = self.db.query(BlogEntity).filter(
                BlogEntity.status == BlogStatus.ACTIVE
            ).all()
            
            logger.info(f"Found {len(active_blogs)} active blogs to crawl")
            
            total_processed = 0
            total_saved = 0
            results = []
            
            for blog in active_blogs:
                try:
                    saved_count = await self.crawl_specific_blog(blog.id)
                    total_saved += saved_count
                    total_processed += 1
                    
                    results.append({
                        "blog_id": blog.id,
                        "blog_name": blog.name,
                        "posts_saved": saved_count
                    })
                    
                    logger.debug(f"Crawled blog: {blog.name} ({blog.company}) - {saved_count} posts saved")
                    
                except Exception as e:
                    logger.error(f"Failed to crawl blog: {blog.name} ({blog.rss_url})", exc_info=e)
                    results.append({
                        "blog_id": blog.id,
                        "blog_name": blog.name,
                        "error": str(e)
                    })
            
            logger.info(f"Crawling completed - {total_processed}/{len(active_blogs)} blogs processed, {total_saved} total posts saved")
            
            return {
                "total_blogs": len(active_blogs),
                "processed_blogs": total_processed,
                "total_posts_saved": total_saved,
                "results": results
            }
            
        except Exception as e:
            logger.error("Error during crawl all active blogs", exc_info=e)
            raise
        finally:
            await self.rss_crawler.close()
    
    async def crawl_specific_blog(self, blog_id: int) -> int:
        """특정 블로그를 크롤링"""
        logger.debug(f"Starting crawl for blog ID: {blog_id}")
        
        try:
            # 블로그 정보 조회
            blog = self.db.query(BlogEntity).filter(
                BlogEntity.id == blog_id,
                BlogEntity.status == BlogStatus.ACTIVE
            ).first()
            
            if not blog:
                raise ValueError(f"Active blog not found with ID: {blog_id}")
            
            # RSS URL 기본 검증
            if not self._is_valid_rss_url(blog.rss_url):
                logger.warning(f"Invalid RSS URL for blog: {blog.name} ({blog.rss_url})")
                return 0
            
            # RSS 피드 크롤링
            crawled_posts = await self.rss_crawler.crawl_feed(blog)
            logger.debug(f"Crawled {len(crawled_posts)} posts from blog: {blog.name}")
            
            saved_count = 0
            for post in crawled_posts:
                try:
                    # 중복 체크
                    existing_post = self.db.query(PostEntity).filter(
                        PostEntity.original_url == post.original_url
                    ).first()
                    
                    if not existing_post:
                        # 포스트 저장
                        self.db.add(post)
                        self.db.commit()
                        self.db.refresh(post)  # ID 가져오기
                        saved_count += 1
                        
                        # 태깅 수행
                        namespace_tags = {}
                        if self.enable_tagging:
                            namespace_tags = await self._tag_post(post)
                        
                        # ElasticSearch 인덱싱
                        if self.enable_indexing:
                            await self._index_post(post, namespace_tags)
                            
                    else:
                        logger.debug(f"Post already exists, skipping: {post.original_url}")
                        
                except Exception as e:
                    logger.error(f"Failed to save post: {post.title} from blog: {blog.name}", exc_info=e)
                    self.db.rollback()
            
            # 마지막 크롤링 시간 업데이트
            from datetime import datetime, timezone
            blog.last_crawled_at = datetime.now(timezone.utc)
            self.db.commit()
            
            logger.debug(f"Saved {saved_count} new posts from blog: {blog.name}")
            return saved_count
            
        except Exception as e:
            logger.error(f"Error crawling blog ID: {blog_id}", exc_info=e)
            self.db.rollback()
            raise
    
    def _is_valid_rss_url(self, rss_url: str) -> bool:
        """RSS URL 기본 검증"""
        if not rss_url or not rss_url.strip():
            return False
        
        if not (rss_url.startswith('http://') or rss_url.startswith('https://')):
            return False
        
        # 기본적인 RSS 관련 키워드 체크
        rss_keywords = ['rss', 'feed', 'atom', '.xml']
        return any(keyword in rss_url.lower() for keyword in rss_keywords)
    
    async def get_crawling_status(self) -> dict:
        """크롤링 상태 조회"""
        try:
            total_blogs = self.db.query(BlogEntity).count()
            active_blogs = self.db.query(BlogEntity).filter(
                BlogEntity.status == BlogStatus.ACTIVE
            ).count()
            total_posts = self.db.query(PostEntity).count()
            
            return {
                "total_blogs": total_blogs,
                "active_blogs": active_blogs,
                "total_posts": total_posts
            }
        except Exception as e:
            logger.error("Error getting crawling status", exc_info=e)
            raise
    
    async def _tag_post(self, post: PostEntity) -> dict:
        """포스트에 대해 태깅 수행"""
        try:
            logger.debug(f"Tagging post: {post.id} - {post.title[:50]}...")
            
            # 제목과 내용 준비
            title = post.title or ""
            content = post.content or ""
            
            if not title.strip() and not content.strip():
                logger.debug(f"Post {post.id} has no content to tag")
                return {}
            
            # 태그 추출
            namespace_tags = tag_classifier.extract_tags_from_text(content, title)
            
            if not namespace_tags:
                logger.debug(f"No tags extracted for post {post.id}")
                return {}
            
            # 태그 정보 로깅
            total_tags = sum(len(tags) for tags in namespace_tags.values())
            logger.debug(f"Extracted {total_tags} tags for post {post.id}: {namespace_tags}")
            
            return namespace_tags
            
        except Exception as e:
            logger.error(f"Failed to tag post {post.id}: {e}")
            # 태깅 실패해도 크롤링은 계속 진행
            return {}
    
    async def _index_post(self, post: PostEntity, namespace_tags: dict = None) -> None:
        """포스트를 ElasticSearch에 인덱싱"""
        try:
            logger.debug(f"Indexing post: {post.id} - {post.title[:50]}...")
            
            success = elasticsearch_indexer.index_post(post, namespace_tags or {})
            
            if success:
                logger.debug(f"Post {post.id} indexed successfully")
            else:
                logger.warning(f"Failed to index post {post.id}")
                
        except Exception as e:
            logger.error(f"Failed to index post {post.id}: {e}")
            # 인덱싱 실패해도 크롤링은 계속 진행
    
    def set_tagging_enabled(self, enabled: bool) -> None:
        """태깅 활성화/비활성화 설정"""
        self.enable_tagging = enabled
        logger.info(f"Tagging {'enabled' if enabled else 'disabled'}")
    
    def set_indexing_enabled(self, enabled: bool) -> None:
        """ElasticSearch 인덱싱 활성화/비활성화 설정"""
        self.enable_indexing = enabled
        logger.info(f"ElasticSearch indexing {'enabled' if enabled else 'disabled'}")
    
    def get_tagging_status(self) -> dict:
        """태깅 상태 정보 반환"""
        return {
            "tagging_enabled": self.enable_tagging,
            "indexing_enabled": self.enable_indexing,
            "tag_classifier_available": True,
            "elasticsearch_available": elasticsearch_indexer.es_client is not None
        }