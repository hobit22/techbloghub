"""
ElasticSearch 인덱싱 서비스
태깅 완료된 포스트를 ElasticSearch에 인덱싱
"""

from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk
from datetime import datetime
from typing import Dict, List, Optional
from loguru import logger

from ..core.config import settings
from ..models import PostEntity


class ElasticSearchIndexer:
    """ElasticSearch 인덱싱 전담 클래스"""
    
    def __init__(self):
        self.es_client = self._create_client()
        self.index_name = "techbloghub_posts"
    
    def _create_client(self):
        """ElasticSearch 클라이언트 생성"""
        try:
            es_client = Elasticsearch(
                [f"http://{settings.elasticsearch_host}:9200"],
                verify_certs=False,
                ssl_show_warn=False,
                request_timeout=30,
                retry_on_timeout=True,
                max_retries=3
            )
            
            if es_client.ping():
                logger.info("ElasticSearch 연결 성공")
                return es_client
            else:
                logger.error("ElasticSearch 연결 실패")
                return None
        except Exception as e:
            logger.error(f"ElasticSearch 클라이언트 생성 오류: {e}")
            return None
    
    def ensure_index_exists(self):
        """인덱스가 존재하지 않으면 생성"""
        if not self.es_client:
            return False
            
        try:
            if not self.es_client.indices.exists(index=self.index_name):
                # 인덱스 설정
                index_settings = {
                    "settings": {
                        "number_of_shards": 1,
                        "number_of_replicas": 0,
                        "analysis": {
                            "analyzer": {
                                "tech_analyzer": {
                                    "type": "custom",
                                    "tokenizer": "keyword",
                                    "filter": ["lowercase"]
                                }
                            }
                        }
                    },
                    "mappings": {
                        "properties": {
                            "post_id": {"type": "integer"},
                            "title": {"type": "text", "analyzer": "standard"},
                            "content": {"type": "text", "analyzer": "standard"},
                            "original_url": {"type": "keyword"},
                            "author": {"type": "text", "analyzer": "standard"},
                            "published_at": {"type": "date"},
                            "created_at": {"type": "date"},
                            "updated_at": {"type": "date"},
                            "indexed_at": {"type": "date"},
                            
                            # 블로그 정보
                            "blog": {
                                "properties": {
                                    "id": {"type": "integer"},
                                    "name": {"type": "text", "analyzer": "standard"},
                                    "company": {"type": "text", "analyzer": "standard"},
                                    "site_url": {"type": "keyword"},
                                    "description": {"type": "text", "analyzer": "standard"}
                                }
                            },
                            
                            # namespace별 태그 필드
                            "language_tags": {"type": "keyword"},
                            "framework_tags": {"type": "keyword"},
                            "database_tags": {"type": "keyword"},
                            "cloud_tags": {"type": "keyword"},
                            "topic_tags": {"type": "keyword"},
                            "company_tags": {"type": "keyword"},
                            "level_tags": {"type": "keyword"},
                            
                            # 통합 태그 정보
                            "all_tags": {
                                "type": "nested",
                                "properties": {
                                    "namespace": {"type": "keyword"},
                                    "tag": {"type": "keyword"},
                                    "confidence": {"type": "float"}
                                }
                            },
                            
                            # 통합 텍스트 필드
                            "all_text": {"type": "text", "analyzer": "standard"},
                            "keywords": {"type": "keyword"}
                        }
                    }
                }
                
                self.es_client.indices.create(index=self.index_name, body=index_settings)
                logger.info(f"인덱스 '{self.index_name}' 생성 완료")
            
            return True
            
        except Exception as e:
            logger.error(f"인덱스 생성/확인 오류: {e}")
            return False
    
    def index_post(self, post_entity: PostEntity, namespace_tags: Dict, keywords: List[str] = None):
        """단일 포스트 인덱싱"""
        if not self.es_client or not self.ensure_index_exists():
            return False
            
        try:
            # ElasticSearch 문서 생성
            doc = self._create_document(post_entity, namespace_tags, keywords)
            
            # 인덱싱
            response = self.es_client.index(
                index=self.index_name,
                id=post_entity.id,
                body=doc
            )
            
            logger.info(f"포스트 {post_entity.id} 인덱싱 완료: {response['result']}")
            return True
            
        except Exception as e:
            logger.error(f"포스트 {post_entity.id} 인덱싱 오류: {e}")
            return False
    
    def bulk_index_posts(self, posts_data: List[tuple]):
        """
        대량 포스트 인덱싱
        posts_data: [(post_entity, namespace_tags, keywords), ...]
        """
        if not self.es_client or not self.ensure_index_exists():
            return 0, len(posts_data)
            
        try:
            actions = []
            for post_entity, namespace_tags, keywords in posts_data:
                doc = self._create_document(post_entity, namespace_tags, keywords)
                
                action = {
                    "_index": self.index_name,
                    "_id": post_entity.id,
                    "_source": doc
                }
                actions.append(action)
            
            success_count, failed_items = bulk(self.es_client, actions)
            
            failed_count = len(failed_items) if failed_items else 0
            logger.info(f"대량 인덱싱 완료: 성공 {success_count}, 실패 {failed_count}")
            
            return success_count, failed_count
            
        except Exception as e:
            logger.error(f"대량 인덱싱 오류: {e}")
            return 0, len(posts_data)
    
    def _create_document(self, post_entity: PostEntity, namespace_tags: Dict, keywords: List[str] = None):
        """ElasticSearch 문서 생성"""
        # 기본 필드
        doc = {
            "post_id": post_entity.id,
            "title": post_entity.title or "",
            "content": post_entity.content or "",
            "original_url": post_entity.original_url,
            "author": post_entity.author,
            "published_at": post_entity.published_at.isoformat() if post_entity.published_at else None,
            "created_at": post_entity.created_at.isoformat() if post_entity.created_at else None,
            "updated_at": post_entity.updated_at.isoformat() if post_entity.updated_at else None,
            "indexed_at": datetime.utcnow().isoformat()
        }
        
        # 블로그 정보
        if post_entity.blog:
            doc["blog"] = {
                "id": post_entity.blog.id,
                "name": post_entity.blog.name,
                "company": post_entity.blog.company,
                "site_url": post_entity.blog.site_url,
                "description": post_entity.blog.description
            }
        
        # namespace별 태그 설정
        if namespace_tags:
            doc["language_tags"] = [tag.tag for tag in namespace_tags.get('language', [])]
            doc["framework_tags"] = [tag.tag for tag in namespace_tags.get('framework', [])]
            doc["database_tags"] = [tag.tag for tag in namespace_tags.get('database', [])]
            doc["cloud_tags"] = [tag.tag for tag in namespace_tags.get('cloud', [])]
            doc["topic_tags"] = [tag.tag for tag in namespace_tags.get('topic', [])]
            doc["company_tags"] = [tag.tag for tag in namespace_tags.get('company', [])]
            doc["level_tags"] = [tag.tag for tag in namespace_tags.get('level', [])]
            
            # 통합 태그 정보
            all_tags = []
            for namespace, tag_results in namespace_tags.items():
                for tag_result in tag_results:
                    all_tags.append({
                        "namespace": namespace,
                        "tag": tag_result.tag,
                        "confidence": tag_result.confidence
                    })
            doc["all_tags"] = all_tags
        
        # 키워드
        if keywords:
            doc["keywords"] = keywords
        
        # 통합 검색 텍스트 생성
        all_text_parts = []
        if doc["title"]:
            all_text_parts.append(doc["title"])
        if doc["content"]:
            all_text_parts.append(doc["content"])
        if doc["author"]:
            all_text_parts.append(doc["author"])
        
        # 태그들도 검색 텍스트에 포함
        for field in ["language_tags", "framework_tags", "database_tags", "cloud_tags", "topic_tags", "company_tags", "level_tags"]:
            if field in doc and doc[field]:
                all_text_parts.extend(doc[field])
        
        doc["all_text"] = " ".join(filter(None, all_text_parts))
        
        return doc
    
    def delete_post(self, post_id: int):
        """포스트 삭제"""
        if not self.es_client:
            return False
            
        try:
            response = self.es_client.delete(index=self.index_name, id=post_id)
            logger.info(f"포스트 {post_id} 삭제 완료: {response['result']}")
            return True
        except Exception as e:
            logger.error(f"포스트 {post_id} 삭제 오류: {e}")
            return False
    
    def get_index_stats(self):
        """인덱스 통계 조회"""
        if not self.es_client:
            return None
            
        try:
            stats = self.es_client.indices.stats(index=self.index_name)
            return {
                "total_documents": stats["_all"]["total"]["docs"]["count"],
                "index_size": stats["_all"]["total"]["store"]["size_in_bytes"]
            }
        except Exception as e:
            logger.error(f"인덱스 통계 조회 오류: {e}")
            return None


# 글로벌 인스턴스
elasticsearch_indexer = ElasticSearchIndexer()