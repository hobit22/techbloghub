from ...models.blog import BlogEntity, BlogType
from .wordpress_crawler import WordPressCrawler
from .naver_d2_crawler import NaverD2Crawler
from .nhn_cloud_crawler import NHNCloudCrawler
from .lycorp_crawler import LYCorpCrawler
from .medium_crawler import MediumCrawler
from .kakao_crawler import KakaoCrawler
from .toss_crawler import TossCrawler


class CrawlerFactory:
    """블로그 타입에 따라 적절한 크롤러를 반환하는 팩토리 클래스"""

    _crawler_map = {
        BlogType.WORDPRESS: WordPressCrawler,
        BlogType.NAVER_D2: NaverD2Crawler,
        BlogType.NHN_CLOUD: NHNCloudCrawler,
        BlogType.LYCORP: LYCorpCrawler,
        BlogType.MEDIUM: MediumCrawler,
        BlogType.KAKAO: KakaoCrawler,
        BlogType.TOSS: TossCrawler,
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
    
    @staticmethod
    def get_supported_types():
        """지원되는 블로그 타입 목록 반환"""
        return [
            {
                "type": BlogType.WORDPRESS.value,
                "description": "WordPress REST API를 지원하는 블로그",
                "examples": ["techblog.woowahan.com", "기타 WordPress 기반 기술블로그"]
            },
            {
                "type": BlogType.NAVER_D2.value,
                "description": "네이버 D2 자체 API를 지원하는 블로그",
                "examples": ["d2.naver.com"]
            },
            {
                "type": BlogType.NHN_CLOUD.value,
                "description": "NHN Cloud Meetup 자체 API를 지원하는 블로그",
                "examples": ["meetup.nhncloud.com"]
            },
            {
                "type": BlogType.LYCORP.value,
                "description": "LY Corporation Gatsby page-data API를 지원하는 블로그",
                "examples": ["techblog.lycorp.co.jp"]
            },
            {
                "type": BlogType.MEDIUM.value,
                "description": "Medium GraphQL API를 지원하는 블로그",
                "examples": ["medium.com/musinsa-tech", "기타 Medium 기반 기술블로그"]
            },
            {
                "type": BlogType.KAKAO.value,
                "description": "카카오 기술블로그 API를 지원하는 블로그",
                "examples": ["tech.kakao.com"]
            },
            {
                "type": BlogType.TOSS.value,
                "description": "토스 기술블로그 API를 지원하는 블로그",
                "examples": ["toss.tech"]
            }
        ]