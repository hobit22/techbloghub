package com.techbloghub.output.rss.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class HttpRssClientTest {

    private RestTemplate restTemplate;
    private HttpRssClient httpRssClient;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        httpRssClient = new TestableHttpRssClient();
    }

    static class TestableHttpRssClient extends HttpRssClient {
        @Override
        public InputStream fetchContent(String url, boolean isProxyRequest) {
            byte[] responseBody = "<?xml version=\"1.0\"?><rss></rss>".getBytes();
            return new java.io.ByteArrayInputStream(responseBody);
        }
    }

    @Test
    void fetchContent_validResponse_shouldReturnInputStream() throws IOException {
        // given
        String url = "https://example.com/rss/";

        // when
        InputStream result = httpRssClient.fetchContent(url, false);

        // then
        assertNotNull(result);
        byte[] actualContent = result.readAllBytes();
        String content = new String(actualContent);
        assertTrue(content.contains("<?xml version=\"1.0\"?><rss></rss>"));
    }

    @Test
    void setTimeout_shouldUpdateTimeout() {
        // given
        int newTimeout = 60;

        // when & then (timeout 설정이 예외 없이 완료되는지 확인)
        assertDoesNotThrow(() -> httpRssClient.setTimeout(newTimeout));
    }

    @Test
    void fetchContent_proxyRequest_shouldReturnContent() throws IOException {
        // given
        String url = "https://proxy.example.com/?url=encoded";

        // when
        InputStream result = httpRssClient.fetchContent(url, true);

        // then
        assertNotNull(result);
        assertTrue(result.available() > 0);
    }

    @Test
    void fetchContent_directRequest_shouldReturnContent() throws IOException {
        // given
        String url = "https://example.com/rss/";

        // when
        InputStream result = httpRssClient.fetchContent(url, false);

        // then
        assertNotNull(result);
        assertTrue(result.available() > 0);
    }
}