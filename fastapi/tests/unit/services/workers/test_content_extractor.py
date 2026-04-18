import unittest

from app.services.workers.content_extractor import ContentExtractor


class ContentExtractorFailureClassificationTests(unittest.TestCase):
    def test_terminal_http_statuses_are_non_retryable(self) -> None:
        extractor = ContentExtractor()

        classification = extractor.classify_failure(
            url="https://example.com/missing",
            status_code=404,
            content_type="text/html",
            exception_message="404 Not Found",
        )

        self.assertEqual("not_found", classification["error"])
        self.assertTrue(classification["terminal"])

    def test_non_html_media_is_terminal(self) -> None:
        extractor = ContentExtractor()

        classification = extractor.classify_failure(
            url="https://example.com/deck.pdf",
            status_code=200,
            content_type="application/pdf",
            exception_message="",
        )

        self.assertEqual("unsupported_content", classification["error"])
        self.assertTrue(classification["terminal"])


if __name__ == "__main__":
    unittest.main()
