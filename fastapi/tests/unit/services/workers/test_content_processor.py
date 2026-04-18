import unittest
from unittest.mock import AsyncMock, MagicMock

from app.models.post import PostStatus
from app.services.workers.content_processor import ContentProcessor


class ContentProcessorFailureHandlingTests(unittest.IsolatedAsyncioTestCase):
    async def test_process_post_marks_terminal_failures_as_non_retryable(self) -> None:
        db = MagicMock()
        db.commit = AsyncMock()

        processor = ContentProcessor(db)
        processor.extractor.extract = AsyncMock(
            return_value={
                "success": False,
                "terminal": True,
                "error": "not_found",
                "message": "404 Not Found",
            }
        )

        post = MagicMock()
        post.id = 1
        post.original_url = "https://example.com/missing-post"
        post.retry_count = 0
        post.status = PostStatus.PENDING
        post.error_message = None

        result = await processor.process_post(post)

        self.assertEqual(PostStatus.FAILED, post.status)
        self.assertEqual(processor.max_retries, post.retry_count)
        self.assertIn("[TERMINAL:not_found]", post.error_message)
        self.assertEqual("terminal_failed", result["error"])

    async def test_process_post_keeps_retryable_failures_retryable(self) -> None:
        db = MagicMock()
        db.commit = AsyncMock()

        processor = ContentProcessor(db)
        processor.extractor.extract = AsyncMock(
            return_value={
                "success": False,
                "terminal": False,
                "error": "timeout",
                "message": "read timeout",
            }
        )

        post = MagicMock()
        post.id = 2
        post.original_url = "https://example.com/slow-post"
        post.retry_count = 1
        post.status = PostStatus.PENDING
        post.error_message = None

        result = await processor.process_post(post)

        self.assertEqual(PostStatus.FAILED, post.status)
        self.assertEqual(2, post.retry_count)
        self.assertEqual("read timeout", post.error_message)
        self.assertEqual("timeout", result["error"])


if __name__ == "__main__":
    unittest.main()
