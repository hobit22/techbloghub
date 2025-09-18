package com.techbloghub.output.rss.adapter;

import com.techbloghub.domain.crawling.model.RssFeed;
import com.techbloghub.domain.crawling.port.FetchRssPort;
import com.techbloghub.output.rss.client.HttpRssClient;
import com.techbloghub.output.rss.parser.RssContentParser;
import com.techbloghub.output.rss.proxy.ProxyUrlResolver;
import com.techbloghub.output.rss.retry.RetryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * HTTP를 통한 RSS 피드 조회 어댑터
 * 여러 컴포넌트를 조합하여 RSS 피드를 가져오는 파사드 역할
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HttpRssFeedAdapter implements FetchRssPort {

    private final ProxyUrlResolver proxyUrlResolver;
    private final HttpRssClient httpRssClient;
    private final RetryHandler retryHandler;
    private final RssContentParser rssContentParser;

    @Override
    public void setTimeout(int timeoutSeconds) {
        httpRssClient.setTimeout(timeoutSeconds);
        log.debug("RSS fetch timeout set to {} seconds", timeoutSeconds);
    }

    @Override
    public RssFeed fetchRssFeed(String rssUrl) {
        log.debug("Fetching RSS feed from: {}", rssUrl);

        ProxyUrlResolver.ProxyResult proxyResult = proxyUrlResolver.resolveProxyUrl(rssUrl);
        String targetUrl = proxyResult.getFinalUrl();
        boolean usingProxy = proxyResult.isUsingProxy();

        log.debug("Target URL: {} (proxy: {})", targetUrl, usingProxy);

        return retryHandler.executeWithRetry(targetUrl, () -> {
            InputStream inputStream = httpRssClient.fetchContent(targetUrl, usingProxy);
            return rssContentParser.parseRssContent(inputStream, proxyResult.getOriginalUrl());
        });
    }
}