"""
Elasticsearch Service Layer
검색 및 인덱싱 비즈니스 로직
"""
import logging
from typing import List, Dict, Any, Optional
from datetime import datetime
from elasticsearch import AsyncElasticsearch
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from app.models import Post
from .mappings import POST_INDEX_NAME

logger = logging.getLogger(__name__)


class ElasticsearchService:
    """Elasticsearch 검색 서비스"""

    def __init__(self, es_client: AsyncElasticsearch, db_session: AsyncSession, index_name: str = POST_INDEX_NAME):
        self.es = es_client
        self.db = db_session
        self.index_name = index_name

    async def search_posts(
        self,
        query: str,
        limit: int = 20,
        offset: int = 0,
        blog_ids: Optional[List[int]] = None
    ) -> tuple[List[Dict[str, Any]], int]:
        """
        Elasticsearch를 사용한 포스트 검색

        Args:
            query: 검색어
            limit: 최대 결과 개수
            offset: 건너뛸 개수 (페이지네이션)
            blog_ids: 블로그 ID 필터 리스트 (optional)

        Returns:
            (검색 결과 리스트, 전체 개수) 튜플
        """
        try:
            # 기본 multi_match 쿼리
            match_query = {
                "multi_match": {
                    "query": query,
                    "fields": [
                        "title^3",      # title 가중치 3배
                        "title.ngram^2",  # title ngram 가중치 2배
                        "content"       # content 기본 가중치
                    ],
                    "type": "best_fields",
                    "operator": "or",
                    "fuzziness": "AUTO"  # 오타 허용
                }
            }

            # blog_ids 필터가 있으면 bool 쿼리로 래핑
            if blog_ids:
                search_query = {
                    "bool": {
                        "must": match_query,
                        "filter": {
                            "terms": {"blog_id": blog_ids}  # terms로 여러 값 필터
                        }
                    }
                }
            else:
                search_query = match_query

            # Elasticsearch 검색 쿼리
            body = {
                "query": search_query,
                "from": offset,
                "size": limit,
                "sort": [
                    {"_score": {"order": "desc"}},
                    {"published_at": {"order": "desc"}}
                ],
                "highlight": {
                    "fields": {
                        "title": {},
                        "content": {
                            "fragment_size": 150,
                            "number_of_fragments": 3
                        }
                    },
                    "pre_tags": ["<mark>"],
                    "post_tags": ["</mark>"]
                }
            }

            # Elasticsearch 검색 실행
            result = await self.es.search(index=self.index_name, body=body)

            # post_id 추출
            post_ids = [hit["_source"]["post_id"] for hit in result["hits"]["hits"]]

            if not post_ids:
                return [], 0

            # PostgreSQL에서 전체 데이터 조회
            posts = await self._fetch_posts_by_ids(post_ids)

            # 결과에 score와 highlight 추가
            posts_dict = {post.id: post for post in posts}
            search_results = []

            for hit in result["hits"]["hits"]:
                post_id = hit["_source"]["post_id"]
                if post_id in posts_dict:
                    post = posts_dict[post_id]
                    post_dict = {
                        "id": post.id,
                        "title": post.title,
                        "content": post.content,
                        "author": post.author,
                        "original_url": post.original_url,
                        "normalized_url": post.normalized_url,
                        "blog_id": post.blog_id,
                        "published_at": post.published_at,
                        "created_at": post.created_at,
                        "updated_at": post.updated_at,
                        "keywords": [],
                        "blog": {
                            "id": post.blog.id,
                            "name": post.blog.name,
                            "company": post.blog.company,
                            "site_url": post.blog.site_url,
                            "logo_url": post.blog.logo_url
                        },
                        "rank": float(hit["_score"]),
                        "highlight": hit.get("highlight", {})
                    }
                    search_results.append(post_dict)

            total = result["hits"]["total"]["value"]
            return search_results, total

        except Exception as e:
            logger.error(f"Elasticsearch search failed: {e}")
            # Elasticsearch 실패 시 빈 결과 반환
            return [], 0

    async def _fetch_posts_by_ids(self, post_ids: List[int]) -> List[Post]:
        """
        PostgreSQL에서 post_id 리스트로 포스트 조회 (순서 유지)
        """
        query = (
            select(Post)
            .options(selectinload(Post.blog))
            .where(Post.id.in_(post_ids))
        )
        result = await self.db.execute(query)
        posts = result.scalars().all()

        # Elasticsearch 결과 순서대로 정렬
        posts_dict = {post.id: post for post in posts}
        ordered_posts = [posts_dict[pid] for pid in post_ids if pid in posts_dict]

        return ordered_posts

    async def index_post(self, post: Post) -> bool:
        """
        단일 포스트 인덱싱

        Args:
            post: 인덱싱할 Post 모델

        Returns:
            성공 여부
        """
        try:
            doc = {
                "post_id": post.id,
                "title": post.title or "",
                "content": post.content or "",
                "author": post.author or "",
                "blog_id": post.blog_id,
                "blog_name": post.blog.name if post.blog else "",
                "blog_company": post.blog.company if post.blog else "",
                "published_at": post.published_at.isoformat() if post.published_at else None,
                "created_at": post.created_at.isoformat() if post.created_at else None,
                "indexed_at": datetime.utcnow().isoformat()
            }

            await self.es.index(
                index=POST_INDEX_NAME,
                id=post.id,
                body=doc
            )
            logger.info(f"Indexed post: {post.id} - {post.title[:50]}")
            return True

        except Exception as e:
            logger.error(f"Failed to index post {post.id}: {e}")
            return False

    async def bulk_index_posts(self, posts: List[Post]) -> tuple[int, int]:
        """
        여러 포스트 일괄 인덱싱

        Args:
            posts: 인덱싱할 Post 모델 리스트

        Returns:
            (성공 개수, 실패 개수) 튜플
        """
        from elasticsearch.helpers import async_bulk

        success_count = 0
        fail_count = 0

        def generate_actions():
            for post in posts:
                yield {
                    "_index": POST_INDEX_NAME,
                    "_id": post.id,
                    "_source": {
                        "post_id": post.id,
                        "title": post.title or "",
                        "content": post.content or "",
                        "author": post.author or "",
                        "blog_id": post.blog_id,
                        "blog_name": post.blog.name if post.blog else "",
                        "blog_company": post.blog.company if post.blog else "",
                        "published_at": post.published_at.isoformat() if post.published_at else None,
                        "created_at": post.created_at.isoformat() if post.created_at else None,
                        "indexed_at": datetime.utcnow().isoformat()
                    }
                }

        try:
            success, failed = await async_bulk(
                self.es,
                generate_actions(),
                raise_on_error=False
            )
            success_count = success
            fail_count = len(failed) if failed else 0

            logger.info(f"Bulk indexed: {success_count} success, {fail_count} failed")

        except Exception as e:
            logger.error(f"Bulk indexing failed: {e}")
            fail_count = len(posts)

        return success_count, fail_count

    async def delete_post(self, post_id: int) -> bool:
        """
        포스트 인덱스 삭제

        Args:
            post_id: 삭제할 포스트 ID

        Returns:
            성공 여부
        """
        try:
            await self.es.delete(index=POST_INDEX_NAME, id=post_id)
            logger.info(f"Deleted post from index: {post_id}")
            return True
        except Exception as e:
            logger.error(f"Failed to delete post {post_id}: {e}")
            return False

    async def get_index_stats(self) -> Dict[str, Any]:
        """
        인덱스 통계 조회

        Returns:
            인덱스 통계 딕셔너리
        """
        try:
            stats = await self.es.indices.stats(index=POST_INDEX_NAME)
            count = await self.es.count(index=POST_INDEX_NAME)

            return {
                "index_name": POST_INDEX_NAME,
                "doc_count": count["count"],
                "store_size": stats["indices"][POST_INDEX_NAME]["total"]["store"]["size_in_bytes"],
                "store_size_mb": round(
                    stats["indices"][POST_INDEX_NAME]["total"]["store"]["size_in_bytes"] / (1024 * 1024), 2
                )
            }
        except Exception as e:
            logger.error(f"Failed to get index stats: {e}")
            return {}
