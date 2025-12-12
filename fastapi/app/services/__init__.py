"""
Services Package
도메인 서비스, 워커, 외부 연동 서비스
"""
# Domain services
from .domain import BlogService, PostService

# Workers
from .workers import RSSCollector, ContentProcessor, ContentExtractor

# Integrations
from .integrations import (
    summary_generator,
    discord_notifier,
    get_es_client,
    init_elasticsearch,
    close_elasticsearch,
    POST_INDEX_NAME,
    POST_INDEX_MAPPING,
    ElasticsearchService,
)

__all__ = [
    # Domain
    "BlogService",
    "PostService",
    # Workers
    "RSSCollector",
    "ContentProcessor",
    "ContentExtractor",
    # Integrations
    "summary_generator",
    "discord_notifier",
    "get_es_client",
    "init_elasticsearch",
    "close_elasticsearch",
    "POST_INDEX_NAME",
    "POST_INDEX_MAPPING",
    "ElasticsearchService",
]
