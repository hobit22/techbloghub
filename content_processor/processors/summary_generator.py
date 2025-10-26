"""
Summary Generator for Content Processing
Generates AI summaries using OpenAI via LangChain
Based on test0925.py template and multi_url_test.ipynb experiments
"""

import logging
import time
from typing import Dict, Any, Optional, List
from dataclasses import dataclass

# LangChain imports
from langchain_openai import ChatOpenAI
from langchain.chains.summarize import load_summarize_chain
from langchain.prompts import PromptTemplate
from langchain_core.documents import Document

# Content sanitization
from utils.content_sanitizer import get_sanitizer


@dataclass
class SummaryResult:
    """Result of summary generation"""
    url: str
    success: bool
    summary: str = ""
    error_message: str = ""
    processing_time: float = 0.0
    input_length: int = 0
    model_used: str = ""
    sanitization_issues: List[str] = None
    quality_score: float = 0.0

    def __post_init__(self):
        if self.sanitization_issues is None:
            self.sanitization_issues = []


class SummaryGenerator:
    """
    AI-powered summary generator using OpenAI
    Implements structured summary format from test0925.py
    """

    def __init__(self, config: Dict[str, Any]):
        self.logger = logging.getLogger(__name__)
        self.config = config

        # OpenAI configuration
        openai_config = config.get('openai', {})
        self.model = openai_config.get('model', 'gpt-4o-mini')
        self.temperature = openai_config.get('temperature', 0.1)
        self.max_tokens = openai_config.get('max_tokens', 4000)

        # Summary templates from config
        summary_config = config.get('summary', {})
        self.template = summary_config.get('template', self._get_default_template())
        self.combine_template = summary_config.get('combine_template', self._get_default_combine_template())

        # Initialize LLM
        self.llm = self._initialize_llm()

        # Initialize summary chain
        self.summary_chain = self._initialize_summary_chain()

        # Initialize content sanitizer
        self.sanitizer = get_sanitizer()

        self.logger.info(f"SummaryGenerator initialized with model: {self.model}")

    def _get_default_template(self) -> str:
        """Default template for individual chunk summarization"""
        return '다음의 내용을 한글로 요약해줘:\n\n{text}'

    def _get_default_combine_template(self) -> str:
        """Default template for combining summaries"""
        return """
        다음 웹 페이지 콘텐츠를 분석하여 핵심 내용을 요약해주세요.
            
웹 페이지 콘텐츠:
{text}

요약 지침:
1. 주요 내용과 핵심 포인트를 3-5개의 불릿 포인트로 요약
2. 기술적 내용이 있다면 중요한 기술 스택이나 개념 포함
3. 실용적이고 유용한 정보 위주로 요약
4. 200-300자 내외로 간결하게 작성
5. 한국어로 작성

요약:
"""

    def _initialize_llm(self) -> ChatOpenAI:
        """Initialize OpenAI LLM"""
        try:
            return ChatOpenAI(
                temperature=self.temperature,
                model_name=self.model,
                max_tokens=self.max_tokens
            )
        except Exception as e:
            self.logger.error(f"Failed to initialize LLM: {e}")
            raise

    def _initialize_summary_chain(self):
        """Initialize LangChain summarization chain"""
        try:
            # Create prompt templates
            prompt = PromptTemplate(template=self.template, input_variables=['text'])
            combine_prompt = PromptTemplate(template=self.combine_template, input_variables=['text'])

            # Create summarization chain
            chain = load_summarize_chain(
                self.llm,
                map_prompt=prompt,
                combine_prompt=combine_prompt,
                chain_type="map_reduce",
                verbose=False
            )

            return chain

        except Exception as e:
            self.logger.error(f"Failed to initialize summary chain: {e}")
            raise

    def _split_content_for_summarization(self, content: str, max_chunk_size: int = 8000) -> List[str]:
        """
        Split content into chunks suitable for LLM processing
        Considers token limits and maintains readability
        """
        if len(content) <= max_chunk_size:
            return [content]

        # Split by paragraphs first
        paragraphs = content.split('\n\n')
        chunks = []
        current_chunk = ""

        for paragraph in paragraphs:
            # If adding this paragraph exceeds limit, start new chunk
            if len(current_chunk) + len(paragraph) + 2 > max_chunk_size:
                if current_chunk:
                    chunks.append(current_chunk.strip())
                    current_chunk = paragraph
                else:
                    # Single paragraph too long, force split
                    chunks.append(paragraph[:max_chunk_size])
                    current_chunk = paragraph[max_chunk_size:]
            else:
                if current_chunk:
                    current_chunk += '\n\n' + paragraph
                else:
                    current_chunk = paragraph

        # Add remaining content
        if current_chunk:
            chunks.append(current_chunk.strip())

        return chunks

    def generate_summary(self, url: str, content: str, max_retries: int = 3) -> SummaryResult:
        """
        Generate summary for a single URL's content with enhanced validation
        """
        start_time = time.time()
        input_length = len(content)

        self.logger.info(f"Generating summary for {url} ({input_length:,} chars)")

        # Validate and sanitize input content
        if not content.strip():
            return SummaryResult(
                url=url,
                success=False,
                error_message="Empty content provided",
                processing_time=time.time() - start_time,
                input_length=input_length,
                model_used=self.model
            )

        # Sanitize content specifically for summary generation
        sanitized_content = self.sanitizer.sanitize_for_summary(content)
        sanitization_issues = []

        if not sanitized_content.strip():
            return SummaryResult(
                url=url,
                success=False,
                error_message="Content became empty after sanitization",
                processing_time=time.time() - start_time,
                input_length=input_length,
                model_used=self.model,
                sanitization_issues=["content_empty_after_sanitization"]
            )

        if len(sanitized_content) != input_length:
            chars_removed = input_length - len(sanitized_content)
            sanitization_issues.append(f"removed_{chars_removed}_characters")
            self.logger.debug(f"Content sanitization for {url}: {input_length} -> {len(sanitized_content)} chars")

        for attempt in range(1, max_retries + 1):
            try:
                # Split sanitized content if too large
                content_chunks = self._split_content_for_summarization(sanitized_content)

                # Create documents for LangChain
                documents = [Document(page_content=chunk) for chunk in content_chunks]

                self.logger.info(f"Processing {len(documents)} chunks for summarization")

                # Generate summary using the chain
                summary = self.summary_chain.invoke({'input_documents': documents})

                # Extract the summary text
                if isinstance(summary, dict) and 'output_text' in summary:
                    summary_text = summary['output_text']
                elif isinstance(summary, str):
                    summary_text = summary
                else:
                    summary_text = str(summary)

                # Validate and sanitize the generated summary
                summary_sanitization_result = self.sanitizer.sanitize_content(summary_text, f"{url}_summary")

                if not summary_sanitization_result['is_valid']:
                    self.logger.error(f"Generated summary sanitization failed for {url}: {summary_sanitization_result.get('error', 'Unknown error')}")
                    raise Exception(f"Summary sanitization failed: {summary_sanitization_result.get('error', 'Invalid summary')}")

                final_summary = summary_sanitization_result['content']

                # Add any summary sanitization issues
                if summary_sanitization_result['issues_found']:
                    sanitization_issues.extend([f"summary: {issue}" for issue in summary_sanitization_result['issues_found']])

                # Calculate quality score
                quality_score = self._calculate_summary_quality(final_summary, sanitized_content)

                processing_time = time.time() - start_time

                self.logger.info(f"✅ Summary generated for {url} ({len(final_summary)} chars, {processing_time:.2f}s, quality: {quality_score:.2f})")

                if sanitization_issues:
                    self.logger.debug(f"Sanitization issues for {url}: {sanitization_issues}")

                return SummaryResult(
                    url=url,
                    success=True,
                    summary=final_summary,
                    processing_time=processing_time,
                    input_length=input_length,
                    model_used=self.model,
                    sanitization_issues=sanitization_issues,
                    quality_score=quality_score
                )

            except Exception as e:
                self.logger.error(f"Summary generation attempt {attempt}/{max_retries} failed: {e}")

                if attempt < max_retries:
                    wait_time = attempt * 2  # Exponential backoff
                    self.logger.info(f"Retrying in {wait_time} seconds...")
                    time.sleep(wait_time)
                else:
                    processing_time = time.time() - start_time
                    return SummaryResult(
                        url=url,
                        success=False,
                        error_message=f"All attempts failed: {str(e)}",
                        processing_time=processing_time,
                        input_length=input_length,
                        model_used=self.model
                    )

    def batch_generate_summaries(
        self,
        url_content_pairs: List[tuple],  # List of (url, content) tuples
        delay_between_requests: float = 2.0
    ) -> List[SummaryResult]:
        """
        Generate summaries for multiple URL-content pairs
        """
        self.logger.info(f"Generating summaries for {len(url_content_pairs)} items")
        results = []

        for i, (url, content) in enumerate(url_content_pairs, 1):
            self.logger.info(f"Processing {i}/{len(url_content_pairs)}: {url}")

            result = self.generate_summary(url, content)
            results.append(result)

            # Rate limiting to avoid API limits
            if i < len(url_content_pairs) and delay_between_requests > 0:
                time.sleep(delay_between_requests)

        # Summary statistics
        successful = [r for r in results if r.success]
        failed = [r for r in results if not r.success]

        self.logger.info(f"Batch summary generation completed:")
        self.logger.info(f"  ✅ Success: {len(successful)}/{len(results)}")
        self.logger.info(f"  ❌ Failed: {len(failed)}/{len(results)}")

        if successful:
            avg_time = sum(r.processing_time for r in successful) / len(successful)
            total_input = sum(r.input_length for r in successful)
            total_output = sum(len(r.summary) for r in successful)

            self.logger.info(f"  ⏱️  Average processing time: {avg_time:.2f}s")
            self.logger.info(f"  📄 Total input: {total_input:,} chars")
            self.logger.info(f"  📄 Total output: {total_output:,} chars")

        return results

    def get_summary_stats(self, results: List[SummaryResult]) -> Dict[str, Any]:
        """Generate summary statistics"""
        successful = [r for r in results if r.success]
        failed = [r for r in results if not r.success]

        return {
            'total_summaries': len(results),
            'successful': len(successful),
            'failed': len(failed),
            'success_rate': (len(successful) / len(results)) * 100 if results else 0,
            'total_input_chars': sum(r.input_length for r in successful),
            'total_output_chars': sum(len(r.summary) for r in successful),
            'average_compression_ratio': (
                sum(len(r.summary) / r.input_length for r in successful if r.input_length > 0) / len(successful)
                if successful else 0
            ),
            'total_processing_time': sum(r.processing_time for r in results),
            'average_processing_time': sum(r.processing_time for r in results) / len(results) if results else 0,
            'model_used': self.model,
            'failed_urls': [r.url for r in failed]
        }

    def _calculate_summary_quality(self, summary: str, original_content: str) -> float:
        """
        Calculate quality score for generated summary (0.0 - 1.0)
        """
        if not summary or not original_content:
            return 0.0

        quality_score = 0.0
        max_score = 1.0

        # 1. Length appropriateness (0.2 points max)
        summary_length = len(summary.strip())
        if 100 <= summary_length <= 500:  # Ideal range
            quality_score += 0.2
        elif 50 <= summary_length < 100 or 500 < summary_length <= 800:  # Acceptable
            quality_score += 0.1
        # else: 0 points for too short or too long

        # 2. Korean language content (0.15 points max)
        korean_chars = sum(1 for char in summary if '\uAC00' <= char <= '\uD7A3')
        korean_ratio = korean_chars / len(summary) if len(summary) > 0 else 0
        if korean_ratio >= 0.3:  # At least 30% Korean characters
            quality_score += 0.15

        # 3. Structure and formatting (0.2 points max)
        structure_score = 0.0
        # Check for bullet points or numbered lists
        if '•' in summary or '-' in summary or any(f'{i}.' in summary for i in range(1, 10)):
            structure_score += 0.1
        # Check for proper sentences (periods)
        if summary.count('.') >= 2:
            structure_score += 0.05
        # Check for proper paragraphs
        if '\n' in summary or len(summary.split('. ')) >= 3:
            structure_score += 0.05
        quality_score += structure_score

        # 4. Content diversity (0.15 points max)
        words = set(summary.lower().split())
        unique_word_ratio = len(words) / len(summary.split()) if len(summary.split()) > 0 else 0
        if unique_word_ratio >= 0.7:  # High vocabulary diversity
            quality_score += 0.15
        elif unique_word_ratio >= 0.5:
            quality_score += 0.1
        elif unique_word_ratio >= 0.3:
            quality_score += 0.05

        # 5. Compression ratio appropriateness (0.15 points max)
        compression_ratio = len(summary) / len(original_content) if len(original_content) > 0 else 0
        if 0.05 <= compression_ratio <= 0.20:  # Ideal compression
            quality_score += 0.15
        elif 0.02 <= compression_ratio < 0.05 or 0.20 < compression_ratio <= 0.30:
            quality_score += 0.1
        elif 0.01 <= compression_ratio < 0.02 or 0.30 < compression_ratio <= 0.50:
            quality_score += 0.05

        # 6. Technical content indicators (0.15 points max)
        tech_indicators = [
            # Programming languages and frameworks
            'javascript', 'python', 'java', 'react', 'node.js', 'spring', 'django',
            # Technical concepts
            'api', 'database', 'frontend', 'backend', '프론트엔드', '백엔드',
            '개발', '코딩', '프로그래밍', '기술', '소프트웨어',
            # Development practices
            'git', 'docker', 'kubernetes', 'ci/cd', 'test', 'testing'
        ]
        summary_lower = summary.lower()
        tech_content_score = min(0.15, sum(0.02 for indicator in tech_indicators if indicator in summary_lower))
        quality_score += tech_content_score

        return min(quality_score, max_score)

    def validate_summary_quality(self, summary: str) -> bool:
        """
        Basic validation of summary quality
        """
        if not summary or len(summary.strip()) < 50:
            return False

        # Check if it contains the expected format elements
        required_sections = ['제목:', '주요내용:', '작성자:', '내용:']
        found_sections = sum(1 for section in required_sections if section in summary)

        # Should have at least 2 of the 4 expected sections
        return found_sections >= 2