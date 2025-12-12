"""
Elasticsearch Integration Module
검색 및 인덱싱 기능
"""
from .client import get_es_client, init_elasticsearch, close_elasticsearch
from .mappings import POST_INDEX_NAME, POST_INDEX_MAPPING
from .service import ElasticsearchService

__all__ = [
    "get_es_client",
    "init_elasticsearch",
    "close_elasticsearch",
    "POST_INDEX_NAME",
    "POST_INDEX_MAPPING",
    "ElasticsearchService",
]
