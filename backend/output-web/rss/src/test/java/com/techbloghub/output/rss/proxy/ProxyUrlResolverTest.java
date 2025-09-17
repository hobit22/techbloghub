package com.techbloghub.output.rss.proxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ProxyUrlResolverTest {

    private ProxyUrlResolver proxyUrlResolver;

    @BeforeEach
    void setUp() {
        proxyUrlResolver = new ProxyUrlResolver();
    }

    @Test
    void resolveProxyUrl_proxyDisabled_shouldReturnDirectResult() {
        // given
        ReflectionTestUtils.setField(proxyUrlResolver, "proxyEnabled", false);
        String originalUrl = "https://techblog.woowahan.com/rss/";

        // when
        ProxyUrlResolver.ProxyResult result = proxyUrlResolver.resolveProxyUrl(originalUrl);

        // then
        assertFalse(result.isUsingProxy());
        assertEquals(originalUrl, result.getOriginalUrl());
        assertEquals(originalUrl, result.getFinalUrl());
    }

    @Test
    void resolveProxyUrl_proxyEnabledButEmptyProxyUrl_shouldReturnDirectResult() {
        // given
        ReflectionTestUtils.setField(proxyUrlResolver, "proxyEnabled", true);
        ReflectionTestUtils.setField(proxyUrlResolver, "proxyUrl", "");
        String originalUrl = "https://techblog.woowahan.com/rss/";

        // when
        ProxyUrlResolver.ProxyResult result = proxyUrlResolver.resolveProxyUrl(originalUrl);

        // then
        assertFalse(result.isUsingProxy());
        assertEquals(originalUrl, result.getOriginalUrl());
        assertEquals(originalUrl, result.getFinalUrl());
    }

    @Test
    void resolveProxyUrl_nonProxyDomain_shouldReturnDirectResult() {
        // given
        ReflectionTestUtils.setField(proxyUrlResolver, "proxyEnabled", true);
        ReflectionTestUtils.setField(proxyUrlResolver, "proxyUrl", "https://proxy.example.com/");
        String originalUrl = "https://other-blog.com/rss/";

        // when
        ProxyUrlResolver.ProxyResult result = proxyUrlResolver.resolveProxyUrl(originalUrl);

        // then
        assertFalse(result.isUsingProxy());
        assertEquals(originalUrl, result.getOriginalUrl());
        assertEquals(originalUrl, result.getFinalUrl());
    }

    @Test
    void resolveProxyUrl_proxyDomain_shouldReturnProxiedResult() {
        // given
        ReflectionTestUtils.setField(proxyUrlResolver, "proxyEnabled", true);
        ReflectionTestUtils.setField(proxyUrlResolver, "proxyUrl", "https://proxy.example.com/?url=");
        String originalUrl = "https://techblog.woowahan.com/rss/";

        // when
        ProxyUrlResolver.ProxyResult result = proxyUrlResolver.resolveProxyUrl(originalUrl);

        // then
        assertTrue(result.isUsingProxy());
        assertEquals(originalUrl, result.getOriginalUrl());
        assertTrue(result.getFinalUrl().startsWith("https://proxy.example.com/?url="));
        assertTrue(result.getFinalUrl().contains("https%3A%2F%2Ftechblog.woowahan.com%2Frss%2F"));
    }

    @Test
    void proxyResult_direct_shouldCreateDirectResult() {
        // given
        String url = "https://example.com/rss/";

        // when
        ProxyUrlResolver.ProxyResult result = ProxyUrlResolver.ProxyResult.direct(url);

        // then
        assertFalse(result.isUsingProxy());
        assertEquals(url, result.getOriginalUrl());
        assertEquals(url, result.getFinalUrl());
    }

    @Test
    void proxyResult_proxied_shouldCreateProxiedResult() {
        // given
        String originalUrl = "https://example.com/rss/";
        String proxiedUrl = "https://proxy.example.com/?url=https%3A%2F%2Fexample.com%2Frss%2F";

        // when
        ProxyUrlResolver.ProxyResult result = ProxyUrlResolver.ProxyResult.proxied(originalUrl, proxiedUrl);

        // then
        assertTrue(result.isUsingProxy());
        assertEquals(originalUrl, result.getOriginalUrl());
        assertEquals(proxiedUrl, result.getFinalUrl());
    }
}