"""
본문 추출 서비스
Failover 전략: Proxy + BeautifulSoup + Trafilatura → Playwright + Trafilatura
"""

import json
import re
from typing import Optional, Dict, Any
from urllib.parse import quote

from bs4 import BeautifulSoup
from playwright.async_api import async_playwright
from trafilatura import fetch_url, extract

from app.core.config import settings


# 노이즈 제거할 CSS 선택자
NOISE_SELECTORS = [
    'nav',
    'footer',
    'aside',
    '.sidebar',
    '.ad',
    '.advertisement',
    '.comment',
    '.social-share',
    '#comments',
    'header',
]


class ContentExtractor:
    """본문 추출 서비스 (Failover 전략 적용)"""

    def __init__(self):
        self.min_content_length = settings.MIN_CONTENT_LENGTH
        self.min_text_ratio = settings.MIN_TEXT_RATIO
        self.proxy_url = settings.RSS_PROXY_URL
        self.playwright_timeout = settings.PLAYWRIGHT_TIMEOUT

    def should_failover(
        self,
        extracted_content: Optional[str],
        html_length: int,
        content_length: int
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
            return True, f"Content too short ({content_length} < {self.min_content_length})"

        # 3. HTML 대비 본문 비율이 너무 낮음
        if html_length > 0 and (content_length / html_length) < self.min_text_ratio:
            ratio = content_length / html_length
            return True, f"Content ratio too low ({ratio:.2%})"

        # 4. 한글/영문 비율 체크 (노이즈만 있는지)
        text_only = re.sub(r'[^가-힣a-zA-Z]', '', extracted_content)
        if len(text_only) < content_length * 0.3:
            return True, "Mostly non-text characters"

        return False, "Valid content"

    def remove_noise(self, html_content: str) -> str:
        """BeautifulSoup로 노이즈 제거"""
        try:
            soup = BeautifulSoup(html_content, 'lxml')

            for selector in NOISE_SELECTORS:
                for element in soup.select(selector):
                    element.decompose()

            return str(soup)
        except Exception:
            # 노이즈 제거 실패 시 원본 반환
            return html_content

    def extract_with_trafilatura(self, html_content: str) -> Optional[Dict[str, Any]]:
        """Trafilatura로 본문 추출 (JSON 형식)"""
        try:
            result_json = extract(
                html_content,
                output_format='json',
                include_comments=False,
                include_tables=True,
                include_images=False,
                include_links=False,
                no_fallback=True,        # True로 변경: 더 정확한 추출
                favor_recall=False,      # False로 변경: 정확도 우선 (title 오추출 방지)
                favor_precision=True,    # 추가: 정확도 우선
                with_metadata=True
            )

            if result_json:
                return json.loads(result_json)

            return None
        except Exception:
            return None

    async def extract_step1(self, url: str, use_proxy: bool = True) -> Optional[Dict[str, Any]]:
        """
        Step 1: Proxy + BeautifulSoup + Trafilatura (빠름)

        Returns:
            추출 결과 또는 None (failover 필요)
        """
        try:
            # HTML 다운로드
            download_url = self.proxy_url + quote(url, safe='') if use_proxy else url
            html_content = fetch_url(download_url)

            if not html_content:
                return None

            html_length = len(html_content)

            # 노이즈 제거
            cleaned_html = self.remove_noise(html_content)

            # Trafilatura로 본문 추출
            data = self.extract_with_trafilatura(cleaned_html)

            if not data:
                return None

            content = data.get('text', '')
            content_length = len(content)

            # Failover 판단
            needs_failover, reason = self.should_failover(content, html_length, content_length)

            if needs_failover:
                return None

            # 성공
            return {
                'method': 'Step 1 (Proxy + BeautifulSoup + Trafilatura)',
                'title': data.get('title'),
                'author': data.get('author'),
                'date': data.get('date'),
                'content': content,
                'content_length': content_length,
            }

        except Exception:
            return None

    async def extract_step2(self, url: str) -> Optional[Dict[str, Any]]:
        """
        Step 2: Playwright + BeautifulSoup + Trafilatura (느리지만 확실)

        Returns:
            추출 결과 또는 None (실패)
        """
        try:
            async with async_playwright() as p:
                browser = await p.chromium.launch(headless=True)
                page = await browser.new_page()
                await page.goto(url, wait_until='networkidle', timeout=self.playwright_timeout)
                html_content = await page.content()
                await browser.close()

                html_length = len(html_content)

                # 노이즈 제거
                cleaned_html = self.remove_noise(html_content)

                # Trafilatura로 본문 추출
                data = self.extract_with_trafilatura(cleaned_html)

                if not data:
                    return None

                content = data.get('text', '')
                content_length = len(content)

                # 성공
                return {
                    'method': 'Step 2 (Playwright + Trafilatura)',
                    'title': data.get('title'),
                    'author': data.get('author'),
                    'date': data.get('date'),
                    'content': content,
                    'content_length': content_length,
                }

        except Exception:
            return None

    async def extract(self, url: str, use_proxy: bool = True) -> Optional[Dict[str, Any]]:
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
            또는 None (모든 방법 실패)
        """
        # Step 1 시도
        result = await self.extract_step1(url, use_proxy=use_proxy)
        if result:
            return result

        # Step 2 시도
        result = await self.extract_step2(url)
        if result:
            return result

        # 모든 방법 실패
        return None
