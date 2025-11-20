package com.techbloghub.output.rss.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@Slf4j
public class ProxyUrlResolver {

    private static final Set<String> PROXY_DOMAINS = Set.of(
            "techblog.woowahan.com",
            "medium.com/feed/musinsa-tech",
            "medium.com/feed/daangn",
            "techblog.gccompany.co.kr/feed"
    );

    @Value("${app.crawler.http.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${app.crawler.http.proxy.url:}")
    private String proxyUrl;

    public ProxyResult resolveProxyUrl(String originalUrl) {
        if (!proxyEnabled || proxyUrl == null || proxyUrl.trim().isEmpty()) {
            log.debug("Proxy disabled or URL not configured");
            return ProxyResult.direct(originalUrl);
        }

        if (!shouldUseProxy(originalUrl)) {
            log.debug("URL {} does not require proxy", originalUrl);
            return ProxyResult.direct(originalUrl);
        }

        try {
            String encodedUrl = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
            String proxiedUrl = proxyUrl + encodedUrl;
            log.info("Using proxy for URL: {} -> {}", originalUrl, proxiedUrl);
            return ProxyResult.proxied(originalUrl, proxiedUrl);
        } catch (Exception e) {
            log.warn("Failed to encode URL for proxy, using direct access: {}", originalUrl, e);
            return ProxyResult.direct(originalUrl);
        }
    }

    private boolean shouldUseProxy(String url) {
        return PROXY_DOMAINS.stream().anyMatch(url::contains);
    }

    public static class ProxyResult {
        private final String originalUrl;
        private final String finalUrl;
        private final boolean usingProxy;

        private ProxyResult(String originalUrl, String finalUrl, boolean usingProxy) {
            this.originalUrl = originalUrl;
            this.finalUrl = finalUrl;
            this.usingProxy = usingProxy;
        }

        public static ProxyResult direct(String url) {
            return new ProxyResult(url, url, false);
        }

        public static ProxyResult proxied(String originalUrl, String proxiedUrl) {
            return new ProxyResult(originalUrl, proxiedUrl, true);
        }

        public String getOriginalUrl() {
            return originalUrl;
        }

        public String getFinalUrl() {
            return finalUrl;
        }

        public boolean isUsingProxy() {
            return usingProxy;
        }
    }
}