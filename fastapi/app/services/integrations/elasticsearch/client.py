"""
Elasticsearch Client Configuration
"""
import logging
from typing import Optional
from elasticsearch import AsyncElasticsearch
from app.core.config import get_settings

logger = logging.getLogger(__name__)

# Global Elasticsearch client
_es_client: Optional[AsyncElasticsearch] = None


def get_es_client() -> AsyncElasticsearch:
    """
    Elasticsearch 클라이언트 가져오기 (Dependency Injection용)
    """
    global _es_client

    if _es_client is None:
        settings = get_settings()
        _es_client = AsyncElasticsearch(
            hosts=[settings.ELASTICSEARCH_URL],
            verify_certs=False,
            max_retries=3,
            retry_on_timeout=True,
        )
        logger.info(f"Elasticsearch client initialized: {settings.ELASTICSEARCH_URL}")

    return _es_client


async def init_elasticsearch() -> None:
    """
    Elasticsearch 초기화 (앱 시작 시 호출)
    """
    from .mappings import POST_INDEX_NAME, POST_INDEX_MAPPING

    es = get_es_client()

    try:
        # 클러스터 연결 확인
        info = await es.info()
        logger.info(f"Connected to Elasticsearch cluster: {info['cluster_name']}")

        # 인덱스 존재 확인
        exists = await es.indices.exists(index=POST_INDEX_NAME)

        if not exists:
            # 인덱스 생성 (Nori 분석기 사용)
            await es.indices.create(
                index=POST_INDEX_NAME,
                body=POST_INDEX_MAPPING
            )
            logger.info(f"Created Elasticsearch index with Nori analyzer: {POST_INDEX_NAME}")
        else:
            logger.info(f"Elasticsearch index already exists: {POST_INDEX_NAME}")

    except Exception as e:
        logger.error(f"Failed to initialize Elasticsearch: {e}")
        raise


async def close_elasticsearch() -> None:
    """
    Elasticsearch 연결 종료 (앱 종료 시 호출)
    """
    global _es_client

    if _es_client:
        await _es_client.close()
        _es_client = None
        logger.info("Elasticsearch client closed")
