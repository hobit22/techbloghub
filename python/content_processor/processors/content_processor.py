"""
Content Processor for Web Scraping
Implements hybrid approach: WebBaseLoader + PlaywrightURLLoader fallback
Based on multi_url_test.ipynb experiments
"""

import logging
import asyncio
import time
from typing import List, Dict, Optional, Tuple, Any
from dataclasses import dataclass

# LangChain imports
from langchain_community.document_loaders import WebBaseLoader, PlaywrightURLLoader
from langchain.text_splitter import CharacterTextSplitter
from langchain_core.documents import Document

# Content sanitization
from utils.content_sanitizer import get_sanitizer

# Async support
import nest_asyncio

# Apply nest_asyncio for Jupyter compatibility
nest_asyncio.apply()


@dataclass
class ProcessingResult:
    """Result of content processing for a single URL"""
    url: str
    success: bool
    content: str = ""
    error_message: str = ""
    method_used: str = ""
    processing_time: float = 0.0
    content_length: int = 0
    chunks_count: int = 0
    sanitization_issues: List[str] = None
    original_content_length: int = 0

    def __post_init__(self):
        if self.sanitization_issues is None:
            self.sanitization_issues = []


class ContentProcessor:
    """
    Hybrid content processor using WebBaseLoader with PlaywrightURLLoader fallback
    Implements lessons learned from multi_url_test.ipynb
    """

    def __init__(self, config: Dict[str, Any]):
        self.logger = logging.getLogger(__name__)
        self.config = config

        # Processing configuration
        self.batch_size = config.get('processing', {}).get('batch_size', 10)
        self.retry_attempts = config.get('processing', {}).get('retry_attempts', 3)
        self.delay_between_requests = config.get('processing', {}).get('delay_between_requests', 1.0)
        self.content_min_length = config.get('processing', {}).get('content_min_length', 1000)
        self.requests_per_second = config.get('processing', {}).get('requests_per_second', 1)
        self.timeout_seconds = config.get('processing', {}).get('timeout_seconds', 30)

        # Content processing settings
        self.chunk_size = config.get('content', {}).get('chunk_size', 3000)
        self.chunk_overlap = config.get('content', {}).get('chunk_overlap', 300)
        self.remove_selectors = config.get('content', {}).get('remove_selectors', [])

        # Initialize text splitter
        self.text_splitter = CharacterTextSplitter(
            separator="\n\n",
            chunk_size=self.chunk_size,
            chunk_overlap=self.chunk_overlap,
            length_function=len,
            is_separator_regex=False,
        )

        # Initialize content sanitizer
        self.sanitizer = get_sanitizer()

    def setup_webbase_loader(self, urls: List[str]) -> WebBaseLoader:
        """Setup WebBaseLoader with optimal configuration"""
        loader = WebBaseLoader(urls)

        # Request settings for stability
        loader.requests_kwargs = {
            'verify': False,  # SSL verification disabled
            'timeout': self.timeout_seconds,
        }

        # Rate limiting
        loader.requests_per_second = self.requests_per_second

        return loader

    async def process_with_webbase_loader(self, urls: List[str]) -> List[ProcessingResult]:
        """
        Process URLs using WebBaseLoader with alazy_load (sequential processing)
        Based on improved method from multi_url_test.ipynb
        """
        self.logger.info(f"Processing {len(urls)} URLs with WebBaseLoader alazy_load")

        results = []
        loader = self.setup_webbase_loader(urls)

        try:
            url_index = 0
            async for doc in loader.alazy_load():
                start_time = time.time()

                try:
                    source_url = doc.metadata.get('source', 'Unknown')
                    if url_index < len(urls):
                        source_url = urls[url_index]

                    content_length = len(doc.page_content)
                    processing_time = time.time() - start_time

                    self.logger.info(f"WebBase: {source_url} ({content_length:,} chars)")

                    if content_length >= self.content_min_length:
                        # Sanitize content before processing
                        sanitization_result = self.sanitizer.sanitize_content(doc.page_content, source_url)

                        if not sanitization_result['is_valid']:
                            results.append(ProcessingResult(
                                url=source_url,
                                success=False,
                                content="",
                                error_message=f"Content sanitization failed: {sanitization_result.get('error', 'Invalid content')}",
                                method_used="WebBaseLoader",
                                processing_time=processing_time,
                                sanitization_issues=sanitization_result['issues_found'],
                                original_content_length=content_length
                            ))
                            self.logger.warning(f"⚠️  Content sanitization failed: {sanitization_result.get('error', 'Invalid content')}")
                        else:
                            sanitized_content = sanitization_result['content']

                            # Split sanitized content into chunks and combine
                            doc.page_content = sanitized_content
                            chunks = self.text_splitter.split_documents([doc])
                            combined_content = "\n\n".join([chunk.page_content for chunk in chunks])

                            results.append(ProcessingResult(
                                url=source_url,
                                success=True,
                                content=combined_content,
                                method_used="WebBaseLoader",
                                processing_time=processing_time,
                                content_length=len(combined_content),
                                chunks_count=len(chunks),
                                sanitization_issues=sanitization_result['issues_found'],
                                original_content_length=content_length
                            ))

                            if sanitization_result['issues_found']:
                                self.logger.info(f"✅ Success with sanitization: {len(chunks)} chunks, {len(combined_content):,} total chars, issues: {sanitization_result['issues_found']}")
                            else:
                                self.logger.info(f"✅ Success: {len(chunks)} chunks, {len(combined_content):,} total chars")

                    else:
                        results.append(ProcessingResult(
                            url=source_url,
                            success=False,
                            content="",
                            error_message=f"Insufficient content: {content_length} chars",
                            method_used="WebBaseLoader",
                            processing_time=processing_time
                        ))

                        self.logger.warning(f"⚠️  Insufficient content: {content_length} chars")

                except Exception as doc_error:
                    processing_time = time.time() - start_time
                    error_url = urls[url_index] if url_index < len(urls) else "Unknown"

                    results.append(ProcessingResult(
                        url=error_url,
                        success=False,
                        content="",
                        error_message=str(doc_error),
                        method_used="WebBaseLoader",
                        processing_time=processing_time
                    ))

                    self.logger.error(f"❌ Document processing error: {doc_error}")

                url_index += 1

                # Rate limiting
                if self.delay_between_requests > 0:
                    await asyncio.sleep(self.delay_between_requests)

        except Exception as e:
            self.logger.error(f"WebBaseLoader alazy_load failed: {e}")

            # Add failed results for remaining URLs
            for i in range(len(results), len(urls)):
                results.append(ProcessingResult(
                    url=urls[i],
                    success=False,
                    content="",
                    error_message=f"WebBaseLoader failed: {str(e)}",
                    method_used="WebBaseLoader"
                ))

        return results

    async def process_with_playwright_loader(self, urls: List[str]) -> List[ProcessingResult]:
        """
        Process URLs using PlaywrightURLLoader for SPA sites
        Fallback method for WebBaseLoader failures
        """
        self.logger.info(f"Processing {len(urls)} URLs with PlaywrightURLLoader")

        results = []

        for url in urls:
            start_time = time.time()

            try:
                loader = PlaywrightURLLoader(
                    urls=[url],
                    remove_selectors=self.remove_selectors,
                    headless=True
                )

                docs = await loader.aload()
                processing_time = time.time() - start_time

                if docs and len(docs) > 0:
                    doc = docs[0]
                    content_length = len(doc.page_content)

                    self.logger.info(f"Playwright: {url} ({content_length:,} chars)")

                    # PlaywrightLoader는 길이에 관계없이 추출된 content가 있으면 성공으로 처리
                    if content_length > 0:
                        # Sanitize content before processing
                        sanitization_result = self.sanitizer.sanitize_content(doc.page_content, url)

                        if not sanitization_result['is_valid']:
                            results.append(ProcessingResult(
                                url=url,
                                success=False,
                                content="",
                                error_message=f"Content sanitization failed: {sanitization_result.get('error', 'Invalid content')}",
                                method_used="PlaywrightURLLoader",
                                processing_time=processing_time,
                                sanitization_issues=sanitization_result['issues_found'],
                                original_content_length=content_length
                            ))
                            self.logger.warning(f"⚠️  Content sanitization failed: {sanitization_result.get('error', 'Invalid content')}")
                        else:
                            sanitized_content = sanitization_result['content']

                            # Split sanitized content into chunks and combine
                            doc.page_content = sanitized_content
                            chunks = self.text_splitter.split_documents([doc])
                            combined_content = "\n\n".join([chunk.page_content for chunk in chunks])

                            results.append(ProcessingResult(
                                url=url,
                                success=True,
                                content=combined_content,
                                method_used="PlaywrightURLLoader",
                                processing_time=processing_time,
                                content_length=len(combined_content),
                                chunks_count=len(chunks),
                                sanitization_issues=sanitization_result['issues_found'],
                                original_content_length=content_length
                            ))

                            # 로그 메시지에서 길이 표시 개선
                            if content_length < self.content_min_length:
                                length_info = f"(short: {content_length} chars)"
                            else:
                                length_info = f"{len(combined_content):,} total chars"

                            if sanitization_result['issues_found']:
                                self.logger.info(f"✅ Success with sanitization: {len(chunks)} chunks, {length_info}, issues: {sanitization_result['issues_found']}")
                            else:
                                self.logger.info(f"✅ Success: {len(chunks)} chunks, {length_info}")

                    else:
                        results.append(ProcessingResult(
                            url=url,
                            success=False,
                            content="",
                            error_message="Empty content extracted",
                            method_used="PlaywrightURLLoader",
                            processing_time=processing_time
                        ))

                        self.logger.warning(f"⚠️  Empty content extracted")

                else:
                    results.append(ProcessingResult(
                        url=url,
                        success=False,
                        content="",
                        error_message="No documents returned",
                        method_used="PlaywrightURLLoader",
                        processing_time=processing_time
                    ))

                    self.logger.error(f"❌ No documents returned for {url}")

            except Exception as e:
                processing_time = time.time() - start_time

                results.append(ProcessingResult(
                    url=url,
                    success=False,
                    content="",
                    error_message=str(e),
                    method_used="PlaywrightURLLoader",
                    processing_time=processing_time
                ))

                self.logger.error(f"❌ PlaywrightURLLoader failed for {url}: {e}")

            # Rate limiting
            if self.delay_between_requests > 0:
                await asyncio.sleep(self.delay_between_requests)

        return results

    async def process_urls_hybrid(self, urls: List[str]) -> List[ProcessingResult]:
        """
        Hybrid processing: WebBaseLoader first, PlaywrightURLLoader for failures
        This is the main processing method
        """
        self.logger.info(f"Starting hybrid processing for {len(urls)} URLs")
        total_start_time = time.time()

        # Step 1: Try WebBaseLoader with alazy_load
        webbase_results = await self.process_with_webbase_loader(urls)

        # Step 2: Identify failures and retry with PlaywrightURLLoader
        failed_urls = [result.url for result in webbase_results if not result.success]

        if failed_urls:
            self.logger.info(f"Retrying {len(failed_urls)} failed URLs with PlaywrightURLLoader")
            playwright_results = await self.process_with_playwright_loader(failed_urls)

            # Replace failed results with PlaywrightURLLoader results
            playwright_results_dict = {result.url: result for result in playwright_results}

            final_results = []
            for result in webbase_results:
                if result.success:
                    final_results.append(result)
                else:
                    # Use PlaywrightURLLoader result if available
                    if result.url in playwright_results_dict:
                        final_results.append(playwright_results_dict[result.url])
                    else:
                        final_results.append(result)  # Keep original failure
        else:
            final_results = webbase_results

        # Summary
        total_time = time.time() - total_start_time
        successful_results = [r for r in final_results if r.success]
        failed_results = [r for r in final_results if not r.success]

        self.logger.info(f"Hybrid processing completed in {total_time:.2f}s:")
        self.logger.info(f"  ✅ Success: {len(successful_results)}/{len(urls)} URLs")
        self.logger.info(f"  ❌ Failed: {len(failed_results)}/{len(urls)} URLs")

        if successful_results:
            total_content = sum(len(r.content) for r in successful_results)
            avg_time = sum(r.processing_time for r in successful_results) / len(successful_results)
            self.logger.info(f"  📄 Total content: {total_content:,} chars")
            self.logger.info(f"  ⏱️  Average processing time: {avg_time:.2f}s/URL")

        return final_results

    async def process_batch(self, urls: List[str]) -> List[ProcessingResult]:
        """
        Process a batch of URLs with retry logic
        """
        for attempt in range(1, self.retry_attempts + 1):
            try:
                self.logger.info(f"Processing batch attempt {attempt}/{self.retry_attempts}")
                return await self.process_urls_hybrid(urls)

            except Exception as e:
                self.logger.error(f"Batch processing attempt {attempt} failed: {e}")

                if attempt < self.retry_attempts:
                    wait_time = attempt * 2  # Exponential backoff
                    self.logger.info(f"Retrying in {wait_time} seconds...")
                    await asyncio.sleep(wait_time)
                else:
                    self.logger.error("All retry attempts failed")
                    # Return failed results for all URLs
                    return [
                        ProcessingResult(
                            url=url,
                            success=False,
                            content="",
                            error_message=f"All retry attempts failed: {str(e)}",
                            method_used="Failed"
                        ) for url in urls
                    ]

    def get_processing_summary(self, results: List[ProcessingResult]) -> Dict[str, Any]:
        """Generate processing summary statistics"""
        successful = [r for r in results if r.success]
        failed = [r for r in results if not r.success]

        method_stats = {}
        for result in successful:
            method = result.method_used
            if method not in method_stats:
                method_stats[method] = 0
            method_stats[method] += 1

        return {
            'total_urls': len(results),
            'successful': len(successful),
            'failed': len(failed),
            'success_rate': (len(successful) / len(results)) * 100 if results else 0,
            'total_content_chars': sum(len(r.content) for r in successful),
            'average_content_length': sum(len(r.content) for r in successful) // len(successful) if successful else 0,
            'total_processing_time': sum(r.processing_time for r in results),
            'average_processing_time': sum(r.processing_time for r in results) / len(results) if results else 0,
            'method_usage': method_stats,
            'failed_urls': [r.url for r in failed]
        }