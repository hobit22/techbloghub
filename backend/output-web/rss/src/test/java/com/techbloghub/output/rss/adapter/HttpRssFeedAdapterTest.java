package com.techbloghub.output.rss.adapter;

import com.techbloghub.domain.model.RssFeed;
import com.techbloghub.output.rss.client.HttpRssClient;
import com.techbloghub.output.rss.parser.RssContentParser;
import com.techbloghub.output.rss.proxy.ProxyUrlResolver;
import com.techbloghub.output.rss.retry.RetryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class HttpRssFeedAdapterTest {

    private TestProxyUrlResolver proxyUrlResolver;
    private TestHttpRssClient httpRssClient;
    private TestRetryHandler retryHandler;
    private TestRssContentParser rssContentParser;
    private HttpRssFeedAdapter adapter;

    @BeforeEach
    void setUp() {
        proxyUrlResolver = new TestProxyUrlResolver();
        httpRssClient = new TestHttpRssClient();
        retryHandler = new TestRetryHandler();
        rssContentParser = new TestRssContentParser();
        adapter = new HttpRssFeedAdapter(proxyUrlResolver, httpRssClient, retryHandler, rssContentParser);
    }

    static class TestProxyUrlResolver extends ProxyUrlResolver {
        private boolean useProxy = false;

        public void setUseProxy(boolean useProxy) {
            this.useProxy = useProxy;
        }

        @Override
        public ProxyResult resolveProxyUrl(String originalUrl) {
            if (useProxy) {
                return ProxyResult.proxied(originalUrl, "https://proxy.example.com/?url=" + originalUrl);
            }
            return ProxyResult.direct(originalUrl);
        }
    }

    static class TestHttpRssClient extends HttpRssClient {
        @Override
        public InputStream fetchContent(String url, boolean isProxyRequest) {
            return new ByteArrayInputStream("<?xml version=\"1.0\"?><rss></rss>".getBytes());
        }
    }

    static class TestRetryHandler extends RetryHandler {
        @Override
        public <T> T executeWithRetry(String url, java.util.function.Supplier<T> operation) {
            return operation.get();
        }
    }

    static class TestRssContentParser extends RssContentParser {
        @Override
        public RssFeed parseRssContent(InputStream inputStream, String rssUrl) {
            return RssFeed.builder()
                    .url(rssUrl)
                    .title("Test Feed")
                    .description("Test Description")
                    .build();
        }
    }

    @Test
    void setTimeout_shouldDelegateToHttpRssClient() {
        // given
        int timeoutSeconds = 60;

        // when & then (timeout 설정이 예외 없이 완료되는지 확인)
        assertDoesNotThrow(() -> adapter.setTimeout(timeoutSeconds));
    }

    @Test
    void fetchRssFeed_directUrl_shouldExecuteCorrectFlow() {
        // given
        String rssUrl = "https://example.com/rss/";
        proxyUrlResolver.setUseProxy(false);

        // when
        RssFeed result = adapter.fetchRssFeed(rssUrl);

        // then
        assertNotNull(result);
        assertEquals(rssUrl, result.getUrl());
        assertEquals("Test Feed", result.getTitle());
        assertEquals("Test Description", result.getDescription());
    }

    @Test
    void fetchRssFeed_proxyUrl_shouldExecuteCorrectFlow() {
        // given
        String originalUrl = "https://techblog.woowahan.com/rss/";
        proxyUrlResolver.setUseProxy(true);

        // when
        RssFeed result = adapter.fetchRssFeed(originalUrl);

        // then
        assertNotNull(result);
        assertEquals(originalUrl, result.getUrl());
        assertEquals("Test Feed", result.getTitle());
        assertEquals("Test Description", result.getDescription());
    }
}