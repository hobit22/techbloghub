"""
Elasticsearch Index Mappings
기본 분석기(Standard Analyzer)와 Nori 한글 형태소 분석기
"""

POST_INDEX_NAME = "posts"
POST_INDEX_NAME_NORI = "posts_nori"

# 기본 분석기 사용 (Standard Analyzer)
POST_INDEX_MAPPING = {
    "settings": {
        "index": {
            "number_of_shards": 1,
            "number_of_replicas": 0,
            "max_result_window": 10000
        }
    },
    "mappings": {
        "properties": {
            "post_id": {
                "type": "integer"
            },
            "title": {
                "type": "text",
                "analyzer": "standard",
                "fields": {
                    "keyword": {
                        "type": "keyword",
                        "ignore_above": 256
                    }
                }
            },
            "content": {
                "type": "text",
                "analyzer": "standard"
            },
            "author": {
                "type": "keyword"
            },
            "blog_id": {
                "type": "integer"
            },
            "blog_name": {
                "type": "keyword"
            },
            "blog_company": {
                "type": "keyword"
            },
            "published_at": {
                "type": "date"
            },
            "created_at": {
                "type": "date"
            },
            "indexed_at": {
                "type": "date"
            }
        }
    }
}

# Nori 한글 형태소 분석기 사용
POST_INDEX_MAPPING_NORI = {
    "settings": {
        "index": {
            "number_of_shards": 1,
            "number_of_replicas": 0,
            "max_result_window": 10000
        },
        "analysis": {
            "tokenizer": {
                "nori_mixed": {
                    "type": "nori_tokenizer",
                    "decompound_mode": "mixed"
                }
            },
            "analyzer": {
                "nori_analyzer": {
                    "type": "custom",
                    "tokenizer": "nori_mixed",
                    "filter": ["lowercase", "nori_readingform"]
                }
            }
        }
    },
    "mappings": {
        "properties": {
            "post_id": {
                "type": "integer"
            },
            "title": {
                "type": "text",
                "analyzer": "nori_analyzer",
                "fields": {
                    "keyword": {
                        "type": "keyword",
                        "ignore_above": 256
                    }
                }
            },
            "content": {
                "type": "text",
                "analyzer": "nori_analyzer"
            },
            "author": {
                "type": "keyword"
            },
            "blog_id": {
                "type": "integer"
            },
            "blog_name": {
                "type": "keyword"
            },
            "blog_company": {
                "type": "keyword"
            },
            "published_at": {
                "type": "date"
            },
            "created_at": {
                "type": "date"
            },
            "indexed_at": {
                "type": "date"
            }
        }
    }
}
