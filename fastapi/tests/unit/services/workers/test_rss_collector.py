import unittest

from app.services.workers.rss_collector import RSSCollector


class RSSCollectorURLResolutionTests(unittest.IsolatedAsyncioTestCase):
    async def test_extract_rss_entries_resolves_relative_urls_with_rss_url(
        self,
    ) -> None:
        collector = RSSCollector(db=None)

        async def fake_fetch_feed_text(rss_url: str, use_proxy: bool) -> str:
            return """
            <rss version="2.0">
              <channel>
                <title>Tech Blog</title>
                <link>https://wrong.example/blog</link>
                <item>
                  <title>Relative entry</title>
                  <link>/posts/2502-exposed-with-mysql-troubleshooting/</link>
                </item>
              </channel>
            </rss>
            """

        collector._fetch_feed_text = fake_fetch_feed_text  # type: ignore[method-assign]

        entries = await collector.extract_rss_entries(
            "https://tech.kakaobank.com/index.xml",
            site_url="https://wrong.example/blog",
        )

        self.assertEqual(1, len(entries))
        self.assertEqual(
            "https://tech.kakaobank.com/posts/2502-exposed-with-mysql-troubleshooting/",
            entries[0]["url"],
        )

    async def test_extract_rss_entries_keeps_absolute_urls(self) -> None:
        collector = RSSCollector(db=None)

        async def fake_fetch_feed_text(rss_url: str, use_proxy: bool) -> str:
            return """
            <rss version="2.0">
              <channel>
                <item>
                  <title>Absolute entry</title>
                  <link>https://tech.kakaobank.com/posts/absolute-entry</link>
                </item>
              </channel>
            </rss>
            """

        collector._fetch_feed_text = fake_fetch_feed_text  # type: ignore[method-assign]

        entries = await collector.extract_rss_entries(
            "https://tech.kakaobank.com/index.xml",
        )

        self.assertEqual(1, len(entries))
        self.assertEqual(
            "https://tech.kakaobank.com/posts/absolute-entry",
            entries[0]["url"],
        )


if __name__ == "__main__":
    unittest.main()
