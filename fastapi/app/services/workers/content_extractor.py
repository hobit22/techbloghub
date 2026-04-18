"""
본문 추출 서비스
Failover 전략: Proxy + BeautifulSoup + Trafilatura → Playwright + Trafilatura
"""

import json
import re
import logging
from html import unescape
from typing import Optional, Dict, Any
from urllib.parse import quote, urlparse

import httpx
from bs4 import BeautifulSoup
from playwright.async_api import async_playwright
from trafilatura import extract

from app.core.config import settings

# 로거 설정
logger = logging.getLogger(__name__)


# 노이즈 제거할 CSS 선택자
NOISE_SELECTORS = [
    "nav",
    "footer",
    "aside",
    "noscript",
    "iframe",
    "script",
    "style",
    ".sidebar",
    ".ad",
    ".advertisement",
    ".comment",
    ".social-share",
    "#comments",
    "header",
    ".related-posts",
    ".recommendation",
    ".newsletter",
    ".share",
]


class ContentExtractor:
    """본문 추출 서비스 (Failover 전략 적용)"""

    def __init__(self):
        self.min_content_length = settings.MIN_CONTENT_LENGTH
        self.min_text_ratio = settings.MIN_TEXT_RATIO
        self.proxy_url = settings.RSS_PROXY_URL
        self.playwright_timeout = settings.PLAYWRIGHT_TIMEOUT

    @staticmethod
    def is_html_content_type(content_type: str) -> bool:
        normalized = (content_type or "").lower()
        return (
            normalized.startswith("text/html")
            or normalized.startswith("application/xhtml+xml")
            or normalized == ""
        )

    def classify_failure(
        self,
        url: str,
        *,
        status_code: Optional[int] = None,
        content_type: str = "",
        exception_message: str = "",
    ) -> Dict[str, Any]:
        message = exception_message or "Unknown extraction failure"
        lowered = message.lower()
        parsed = urlparse(url)

        if parsed.scheme not in ("http", "https") or not parsed.netloc:
            return {
                "success": False,
                "terminal": True,
                "error": "invalid_url",
                "message": message,
            }

        if status_code in (404, 410):
            return {
                "success": False,
                "terminal": True,
                "error": "not_found",
                "message": message,
            }

        if status_code in (401, 403):
            return {
                "success": False,
                "terminal": True,
                "error": "access_denied",
                "message": message,
            }

        if content_type and not self.is_html_content_type(content_type):
            return {
                "success": False,
                "terminal": True,
                "error": "unsupported_content",
                "message": f"Unsupported content type: {content_type}",
            }

        if (
            "missing an 'http://' or 'https://' protocol" in lowered
            or "cannot navigate to invalid url" in lowered
        ):
            return {
                "success": False,
                "terminal": True,
                "error": "invalid_url",
                "message": message,
            }

        if status_code == 429 or "timeout" in lowered:
            return {
                "success": False,
                "terminal": False,
                "error": "timeout",
                "message": message,
            }

        return {
            "success": False,
            "terminal": False,
            "error": "extraction_failed",
            "message": message,
        }

    @staticmethod
    def _build_request_headers() -> Dict[str, str]:
        return {
            "User-Agent": (
                "Mozilla/5.0 (compatible; TechBlogHubBot/1.0; "
                "+https://github.com/hobit22/techbloghub)"
            ),
            "Accept-Language": "ko,en-US;q=0.9,en;q=0.8",
        }

    @staticmethod
    def normalize_content(content: str) -> str:
        normalized = unescape(content or "")
        normalized = normalized.replace("\u200b", "").replace("\ufeff", "")
        normalized = normalized.replace("\r\n", "\n")
        normalized = re.sub(r"\n{3,}", "\n\n", normalized)
        normalized = re.sub(r"[ \t]{2,}", " ", normalized)
        return normalized.strip()

    def should_failover(
        self, extracted_content: Optional[str], html_length: int, content_length: int
    ) -> tuple[bool, str]:
        """
        Failover 여부 판단

        Returns:
            (needs_failover, reason)
        """
        # 1. 추출 실패
        if not extracted_content:
            return True, "Extraction returned None"

        # 2. 본문이 너무 짧음
        if content_length < self.min_content_length:
            return (
                True,
                f"Content too short ({content_length} < {self.min_content_length})",
            )

        # 3. HTML 대비 본문 비율이 너무 낮음
        if html_length > 0 and (content_length / html_length) < self.min_text_ratio:
            ratio = content_length / html_length
            return True, f"Content ratio too low ({ratio:.2%})"

        # 4. 한글/영문 비율 체크 (노이즈만 있는지)
        text_only = re.sub(r"[^가-힣a-zA-Z]", "", extracted_content)
        if len(text_only) < content_length * 0.3:
            return True, "Mostly non-text characters"

        return False, "Valid content"

    def remove_noise(self, html_content: str) -> str:
        """BeautifulSoup로 노이즈 제거"""
        try:
            soup = BeautifulSoup(html_content, "lxml")

            for selector in NOISE_SELECTORS:
                for element in soup.select(selector):
                    element.decompose()

            return str(soup)
        except Exception as e:
            logger.warning(f"Failed to remove noise: {e}")
            # 노이즈 제거 실패 시 원본 반환
            return html_content

    def extract_with_trafilatura(self, html_content: str) -> Optional[Dict[str, Any]]:
        """Trafilatura로 본문 추출 (JSON 형식)"""
        try:
            result_json = extract(
                html_content,
                output_format="json",
                include_comments=False,
                include_tables=True,
                include_images=False,
                include_links=False,
                no_fallback=True,  # True로 변경: 더 정확한 추출
                favor_recall=False,  # False로 변경: 정확도 우선 (title 오추출 방지)
                favor_precision=True,  # 추가: 정확도 우선
                with_metadata=True,
            )

            if result_json:
                return json.loads(result_json)

            return None
        except Exception as e:
            logger.error(f"Trafilatura extraction failed: {e}")
            return None

    async def extract_step1(self, url: str, use_proxy: bool = True) -> Dict[str, Any]:
        """
        Step 1: Proxy + BeautifulSoup + Trafilatura (빠름)

        Returns:
            추출 결과 또는 실패 메타데이터
        """
        try:
            # HTML 다운로드 (비동기)
            download_url = self.proxy_url + quote(url, safe="") if use_proxy else url

            async with httpx.AsyncClient(
                timeout=httpx.Timeout(30.0, connect=10.0),
                follow_redirects=True,
                headers=self._build_request_headers(),
            ) as client:
                response = await client.get(download_url)
                response.raise_for_status()
                html_content = response.text
                content_type = response.headers.get("content-type", "")

            if content_type and not self.is_html_content_type(content_type):
                return self.classify_failure(
                    url,
                    status_code=response.status_code,
                    content_type=content_type,
                    exception_message=f"Unsupported content type: {content_type}",
                )

            if not html_content:
                return self.classify_failure(
                    url,
                    content_type=content_type,
                    exception_message="Empty HTML response",
                )

            html_length = len(html_content)

            # 노이즈 제거
            cleaned_html = self.remove_noise(html_content)

            # Trafilatura로 본문 추출
            data = self.extract_with_trafilatura(cleaned_html)

            if not data:
                return self.classify_failure(
                    url,
                    content_type=content_type,
                    exception_message="Trafilatura returned None",
                )

            content = self.normalize_content(data.get("text", ""))
            content_length = len(content)

            # Failover 판단
            needs_failover, reason = self.should_failover(
                content, html_length, content_length
            )

            if needs_failover:
                return self.classify_failure(
                    url,
                    content_type=content_type,
                    exception_message=reason,
                )

            # 성공
            logger.info(
                f"Step 1 succeeded for {url[:50]}... (length: {content_length})"
            )
            return {
                "success": True,
                "method": "Step 1 (Proxy + BeautifulSoup + Trafilatura)",
                "title": data.get("title"),
                "author": data.get("author"),
                "date": data.get("date"),
                "content": content,
                "content_length": content_length,
            }

        except httpx.HTTPStatusError as e:
            logger.warning(f"Step 1 failed for {url[:50]}...: {e}")
            return self.classify_failure(
                url,
                status_code=e.response.status_code,
                content_type=e.response.headers.get("content-type", ""),
                exception_message=str(e),
            )
        except Exception as e:
            logger.warning(f"Step 1 failed for {url[:50]}...: {e}")
            return self.classify_failure(url, exception_message=str(e))

    async def extract_step2(self, url: str) -> Dict[str, Any]:
        """
        Step 2: Playwright + BeautifulSoup + Trafilatura (느리지만 확실)

        Returns:
            추출 결과 또는 실패 메타데이터
        """
        browser = None
        try:
            async with async_playwright() as p:
                browser = await p.chromium.launch(headless=True)
                page = await browser.new_page(
                    user_agent=self._build_request_headers()["User-Agent"]
                )
                await page.goto(
                    url, wait_until="domcontentloaded", timeout=self.playwright_timeout
                )
                await page.wait_for_timeout(1500)
                html_content = await page.content()

                html_length = len(html_content)

                # 노이즈 제거
                cleaned_html = self.remove_noise(html_content)

                # Trafilatura로 본문 추출
                data = self.extract_with_trafilatura(cleaned_html)

                if not data:
                    return self.classify_failure(
                        url,
                        exception_message="Trafilatura returned None after browser render",
                    )

                content = self.normalize_content(data.get("text", ""))
                content_length = len(content)

                # 성공
                logger.info(
                    f"Step 2 succeeded for {url[:50]}... (length: {content_length})"
                )
                return {
                    "success": True,
                    "method": "Step 2 (Playwright + Trafilatura)",
                    "title": data.get("title"),
                    "author": data.get("author"),
                    "date": data.get("date"),
                    "content": content,
                    "content_length": content_length,
                }

        except Exception as e:
            logger.error(f"Step 2 failed for {url[:50]}...: {e}", exc_info=True)
            return self.classify_failure(url, exception_message=str(e))
        finally:
            if browser:
                try:
                    await browser.close()
                except Exception as close_error:
                    logger.warning(f"Failed to close browser: {close_error}")

    async def extract(self, url: str, use_proxy: bool = True) -> Dict[str, Any]:
        """
        Failover 전략으로 본문 추출

        Args:
            url: 추출할 URL
            use_proxy: Cloudflare 프록시 사용 여부 (기본값: True)

        Returns:
            {
                'method': str,
                'title': str,
                'author': str,
                'date': str,
                'content': str,
                'content_length': int,
            }
            또는 실패 메타데이터 dict
        """
        # Step 1 시도
        logger.info(f"Starting content extraction for {url[:50]}...")
        result = await self.extract_step1(url, use_proxy=use_proxy)
        if result.get("success"):
            return result

        if result.get("terminal"):
            logger.error(f"Terminal extraction failure for {url}: {result['error']}")
            return result

        if use_proxy and self.proxy_url:
            logger.info(f"Proxy fetch failed, retrying direct fetch for {url[:50]}...")
            result = await self.extract_step1(url, use_proxy=False)
            if result.get("success"):
                return result
            if result.get("terminal"):
                logger.error(
                    f"Terminal extraction failure for {url}: {result['error']}"
                )
                return result

        logger.info(f"Step 1 failed, trying Step 2 for {url[:50]}...")
        result = await self.extract_step2(url)
        if result.get("success"):
            return result

        # 모든 방법 실패
        logger.error(f"All extraction methods failed for {url}")
        return result
