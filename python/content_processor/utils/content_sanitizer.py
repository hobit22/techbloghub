"""
Content Sanitizer for Database-Safe Text Processing
Handles NUL characters, binary data, and encoding issues
"""

import logging
import re
from typing import Optional, Dict, Any
import unicodedata


class ContentSanitizer:
    """
    Sanitizes web content for safe database storage and processing
    """

    def __init__(self):
        self.logger = logging.getLogger(__name__)

        # Compile regex patterns for efficiency
        self.nul_pattern = re.compile(r'\x00')
        self.control_chars_pattern = re.compile(r'[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]')
        self.binary_pattern = re.compile(r'[\x00-\x08\x0B\x0C\x0E-\x1F\x7F-\x9F]')

        # Common binary file signatures (magic numbers)
        self.binary_signatures = [
            b'\xFF\xD8\xFF',  # JPEG
            b'\x89\x50\x4E\x47',  # PNG
            b'\x47\x49\x46',  # GIF
            b'\x25\x50\x44\x46',  # PDF
            b'\x50\x4B\x03\x04',  # ZIP
            b'\x52\x61\x72\x21',  # RAR
            b'\x7F\x45\x4C\x46',  # ELF
        ]

    def sanitize_content(self, content: str, url: str = "Unknown") -> Dict[str, Any]:
        """
        Main sanitization function
        Returns dict with sanitized content and metadata
        """
        if not content:
            return {
                'content': '',
                'is_valid': True,
                'issues_found': [],
                'original_length': 0,
                'sanitized_length': 0
            }

        original_length = len(content)
        issues_found = []

        # Step 1: Check for binary content
        if self._is_likely_binary(content):
            self.logger.warning(f"Binary content detected in {url}")
            issues_found.append('binary_content_detected')
            return {
                'content': '',
                'is_valid': False,
                'issues_found': issues_found,
                'original_length': original_length,
                'sanitized_length': 0,
                'error': 'Binary content detected'
            }

        # Step 2: Remove NUL characters
        nul_count = len(self.nul_pattern.findall(content))
        if nul_count > 0:
            content = self.nul_pattern.sub('', content)
            issues_found.append(f'removed_{nul_count}_nul_characters')
            self.logger.warning(f"Removed {nul_count} NUL characters from {url}")

        # Step 3: Handle other control characters
        control_chars = self.control_chars_pattern.findall(content)
        if control_chars:
            content = self.control_chars_pattern.sub(' ', content)
            issues_found.append(f'replaced_{len(control_chars)}_control_characters')
            self.logger.debug(f"Replaced {len(control_chars)} control characters from {url}")

        # Step 4: Normalize Unicode
        try:
            content = unicodedata.normalize('NFKC', content)
        except Exception as e:
            self.logger.error(f"Unicode normalization failed for {url}: {e}")
            issues_found.append('unicode_normalization_failed')

        # Step 5: Ensure valid UTF-8
        content = self._ensure_valid_utf8(content, url, issues_found)

        # Step 6: Clean up excessive whitespace
        content = self._clean_whitespace(content)

        # Step 7: Final validation
        is_valid = self._validate_final_content(content)

        sanitized_length = len(content)

        if issues_found:
            self.logger.info(f"Sanitized content from {url}: {original_length} → {sanitized_length} chars, issues: {issues_found}")

        return {
            'content': content,
            'is_valid': is_valid,
            'issues_found': issues_found,
            'original_length': original_length,
            'sanitized_length': sanitized_length
        }

    def _is_likely_binary(self, content: str) -> bool:
        """
        Check if content is likely binary data
        """
        # Check for binary signatures in the first 1024 characters
        sample = content[:1024].encode('utf-8', errors='ignore')

        for signature in self.binary_signatures:
            if sample.startswith(signature):
                return True

        # Check for high ratio of non-printable characters
        if len(content) > 100:
            non_printable_count = sum(1 for char in content[:1000] if ord(char) < 32 and char not in '\t\n\r')
            ratio = non_printable_count / min(len(content), 1000)
            if ratio > 0.1:  # More than 10% non-printable characters
                return True

        # Check for very high ratio of control characters
        control_char_count = len(self.binary_pattern.findall(content[:1000]))
        if control_char_count > 50:  # Arbitrary threshold
            return True

        return False

    def _ensure_valid_utf8(self, content: str, url: str, issues_found: list) -> str:
        """
        Ensure content is valid UTF-8
        """
        try:
            # Try to encode and decode to catch any encoding issues
            encoded = content.encode('utf-8')
            decoded = encoded.decode('utf-8')
            return decoded
        except (UnicodeEncodeError, UnicodeDecodeError) as e:
            self.logger.warning(f"UTF-8 encoding issue in {url}: {e}")
            issues_found.append('utf8_encoding_fixed')

            # Try to fix by encoding with error handling
            try:
                fixed_content = content.encode('utf-8', errors='ignore').decode('utf-8')
                return fixed_content
            except Exception as fix_error:
                self.logger.error(f"Failed to fix UTF-8 encoding for {url}: {fix_error}")
                issues_found.append('utf8_encoding_failed')
                return ""

    def _clean_whitespace(self, content: str) -> str:
        """
        Clean up excessive whitespace while preserving structure
        """
        # Replace multiple consecutive spaces with single space
        content = re.sub(r' +', ' ', content)

        # Replace multiple consecutive newlines with max 2 newlines
        content = re.sub(r'\n\s*\n\s*\n+', '\n\n', content)

        # Remove trailing whitespace from lines
        lines = content.split('\n')
        lines = [line.rstrip() for line in lines]
        content = '\n'.join(lines)

        # Remove leading and trailing whitespace from entire content
        content = content.strip()

        return content

    def _validate_final_content(self, content: str) -> bool:
        """
        Final validation of sanitized content
        """
        if not content or len(content.strip()) < 10:
            return False

        # Check for remaining problematic characters
        if '\x00' in content:
            return False

        # Check if content is mostly readable text
        printable_chars = sum(1 for char in content[:1000] if char.isprintable() or char in '\t\n\r')
        if len(content) > 100:
            printable_ratio = printable_chars / min(len(content), 1000)
            if printable_ratio < 0.8:  # Less than 80% printable characters
                return False

        return True

    def sanitize_for_summary(self, content: str, max_length: int = 50000) -> str:
        """
        Sanitize content specifically for AI summary generation
        More aggressive cleaning for LLM processing
        """
        if not content:
            return ""

        # First apply standard sanitization
        result = self.sanitize_content(content)
        if not result['is_valid']:
            return ""

        content = result['content']

        # Truncate if too long for LLM processing
        if len(content) > max_length:
            content = content[:max_length]
            # Try to cut at a sentence boundary
            last_sentence_end = max(
                content.rfind('.'),
                content.rfind('!'),
                content.rfind('?')
            )
            if last_sentence_end > max_length * 0.8:  # If we can cut reasonably close to the end
                content = content[:last_sentence_end + 1]

        # Remove very long lines that might be code or data
        lines = content.split('\n')
        filtered_lines = []
        for line in lines:
            if len(line) <= 500:  # Keep lines under 500 characters
                filtered_lines.append(line)
            else:
                # For very long lines, check if they seem like natural language
                words = line.split()
                if len(words) > 20 and len(line) / len(words) < 50:  # Average word length < 50
                    # Seems like natural language, keep it but truncate
                    filtered_lines.append(line[:500] + '...')

        return '\n'.join(filtered_lines)

    def get_content_stats(self, content: str) -> Dict[str, Any]:
        """
        Get detailed statistics about content
        Useful for debugging and monitoring
        """
        if not content:
            return {'length': 0, 'is_empty': True}

        stats = {
            'length': len(content),
            'lines': len(content.split('\n')),
            'words': len(content.split()),
            'is_empty': False,
            'encoding_info': {},
            'character_analysis': {}
        }

        # Character analysis
        printable_count = sum(1 for char in content if char.isprintable())
        control_count = len(self.control_chars_pattern.findall(content))
        nul_count = content.count('\x00')

        stats['character_analysis'] = {
            'printable_characters': printable_count,
            'control_characters': control_count,
            'nul_characters': nul_count,
            'printable_ratio': printable_count / len(content) if len(content) > 0 else 0
        }

        # Encoding info
        try:
            encoded = content.encode('utf-8')
            stats['encoding_info'] = {
                'utf8_bytes': len(encoded),
                'utf8_valid': True,
                'compression_ratio': len(encoded) / len(content) if len(content) > 0 else 0
            }
        except UnicodeEncodeError:
            stats['encoding_info'] = {
                'utf8_valid': False,
                'encoding_error': True
            }

        return stats


# Global sanitizer instance
_sanitizer_instance = None

def get_sanitizer() -> ContentSanitizer:
    """Get global sanitizer instance"""
    global _sanitizer_instance
    if _sanitizer_instance is None:
        _sanitizer_instance = ContentSanitizer()
    return _sanitizer_instance

def sanitize_content(content: str, url: str = "Unknown") -> Dict[str, Any]:
    """Convenience function for content sanitization"""
    return get_sanitizer().sanitize_content(content, url)