from typing import Optional
from ...models.blog import BlogEntity, BlogType
from .wordpress_crawler import WordPressCrawler
from .naver_d2_crawler import NaverD2Crawler
from .nhn_cloud_crawler import NHNCloudCrawler


class CrawlerFactory:
    """블로그 타입에 따라 적절한 크롤러를 반환하는 팩토리 클래스"""

    _crawler_map = {
        BlogType.WORDPRESS: WordPressCrawler,
        BlogType.NAVER_D2: NaverD2Crawler,
        BlogType.NHN_CLOUD: NHNCloudCrawler,
    }

    @staticmethod
    def create_crawler(blog: BlogEntity):
        """블로그 엔티티에 따라 적절한 크롤러 인스턴스를 생성"""
        if not blog.blog_type:
            raise ValueError(f"Blog {blog.name} does not have a blog_type set for backfilling")

        crawler_cls = CrawlerFactory._crawler_map.get(blog.blog_type)
        if not crawler_cls:
            raise ValueError(f"Unsupported blog type: {blog.blog_type}")

        return crawler_cls()