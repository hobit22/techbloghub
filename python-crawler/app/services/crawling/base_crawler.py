from abc import ABC, abstractmethod
from typing import List, Protocol, Dict
from ...models import BlogEntity, PostEntity


class BaseCrawler(Protocol):
    """크롤러 인터페이스 정의"""
    
    async def backfill_all_posts(self, blog: BlogEntity) -> List[PostEntity]:
        """블로그의 모든 포스트를 백필링"""
        ...
    
    async def backfill_and_save_to_db(self, blog: BlogEntity) -> Dict[str, int]:
        """블로그를 백필링하고 DB에 저장"""
        ...
    
    async def close(self) -> None:
        """리소스 정리"""
        ...


class AsyncHTTPCrawler(ABC):
    """HTTP 클라이언트를 사용하는 크롤러의 기본 추상 클래스"""
    
    def __init__(self):
        """서브클래스에서 self.client를 초기화해야 함"""
        self.client = None
    
    @abstractmethod
    async def backfill_all_posts(self, blog: BlogEntity) -> List[PostEntity]:
        """블로그의 모든 포스트를 백필링 (서브클래스에서 구현)"""
        pass
    
    async def close(self) -> None:
        """HTTP 클라이언트 종료"""
        if self.client:
            await self.client.aclose()