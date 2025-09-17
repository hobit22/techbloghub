package com.techbloghub.output.rss.adapter;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techbloghub.domain.model.RssEntry;
import com.techbloghub.domain.model.RssFeed;
import com.techbloghub.domain.port.out.FetchRssPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;

/**
 * HTTP를 통한 RSS 피드 조회 어댑터
 */
@Component
@Slf4j
public class HttpRssFeedAdapter implements FetchRssPort {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int BASE_DELAY_MS = 1000;

    @Value("${app.crawler.http.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${app.crawler.http.proxy.url:}")
    private String proxyUrl;

    private int timeoutSeconds = 30;

    private final RestTemplate restTemplate;

    public HttpRssFeedAdapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void setTimeout(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        log.debug("RSS fetch timeout set to {} seconds", timeoutSeconds);
    }

    @Override
    public RssFeed fetchRssFeed(String rssUrl) {
        String originalUrl = rssUrl;
        boolean usingProxy = false;

        // 우아한형제들 URL인 경우 프록시 사용
        if (proxyEnabled && !proxyUrl.isEmpty() && rssUrl.contains("techblog.woowahan.com")) {
            try {
                rssUrl = proxyUrl + URLEncoder.encode(rssUrl, StandardCharsets.UTF_8);
                usingProxy = true;
                log.info("Using CloudFlare Workers proxy for Woowahan RSS: {} -> {}", originalUrl, rssUrl);
            } catch (Exception e) {
                log.warn("Failed to encode URL for proxy, using original URL: {}", originalUrl, e);
                rssUrl = originalUrl;
            }
        }

        log.debug("Fetching RSS feed from: {} (proxy: {})", rssUrl, usingProxy);

        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                if (attempt > 1) {
                    long delay = calculateExponentialBackoffDelay(attempt);
                    log.info("Retrying RSS fetch attempt {}/{} for {} after {}ms delay", 
                            attempt, MAX_RETRY_ATTEMPTS, rssUrl, delay);
                    Thread.sleep(delay);
                }

                InputStream inputStream = fetchRssContent(rssUrl);
                
                if (inputStream == null) {
                    throw new RssFetchException("Empty RSS content received from: " + rssUrl);
                }

                return parseRssContentFromStream(inputStream, originalUrl);

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                lastException = e;
                
                if (e.getStatusCode().value() == 403 && attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("Received 403 Forbidden for {}, attempt {}/{}. Will retry with different headers.", 
                            rssUrl, attempt, MAX_RETRY_ATTEMPTS);
                    continue;
                } else {
                    String errorMsg = String.format("HTTP error %d for RSS URL [%s]: %s",
                            e.getStatusCode().value(), rssUrl, e.getMessage());
                    log.error(errorMsg, e);
                    throw new RssFetchException(errorMsg, e);
                }
                
            } catch (ResourceAccessException e) {
                lastException = e;
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("Request failed for {}, attempt {}/{}. Will retry.", 
                            rssUrl, attempt, MAX_RETRY_ATTEMPTS);
                    continue;
                } else {
                    String errorMsg = String.format("Request failed for RSS URL [%s]: %s", rssUrl, e.getMessage());
                    log.error(errorMsg, e);
                    throw new RssFetchException(errorMsg, e);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RssFetchException("Thread interrupted while waiting to retry: " + rssUrl, e);
                
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("Failed to fetch RSS feed from {}, attempt {}/{}. Will retry.", 
                            rssUrl, attempt, MAX_RETRY_ATTEMPTS);
                    continue;
                } else {
                    String errorMsg = String.format("Failed to fetch RSS feed from [%s]: %s", rssUrl, e.getMessage());
                    log.error(errorMsg, e);
                    throw new RssFetchException(errorMsg, e);
                }
            }
        }
        
        // 모든 재시도가 실패한 경우
        String errorMsg = String.format("Failed to fetch RSS feed from [%s] after %d attempts", rssUrl, MAX_RETRY_ATTEMPTS);
        log.error(errorMsg, lastException);
        throw new RssFetchException(errorMsg, lastException);
    }

    private InputStream fetchRssContent(String rssUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        headers.set("Accept", "application/rss+xml, application/xml, text/xml, */*");
        headers.set("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8");

        // 프록시 요청이 아닌 경우에만 압축 허용
        if (!rssUrl.contains("workers.dev")) {
            headers.set("Accept-Encoding", "gzip, deflate, br");
        }

        headers.set("Cache-Control", "no-cache");
        headers.set("Connection", "keep-alive");
        headers.set("Referer", "https://www.google.com/");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                rssUrl,
                HttpMethod.GET,
                entity,
                byte[].class
        );

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new RssFetchException("Empty RSS content received from: " + rssUrl);
        }

        return new ByteArrayInputStream(body);
    }

    private long calculateExponentialBackoffDelay(int attempt) {
        long baseDelay = BASE_DELAY_MS * (long) Math.pow(2, attempt - 1);
        long jitter = ThreadLocalRandom.current().nextLong(0, baseDelay / 2);
        return baseDelay + jitter;
    }

    /**
     * InputStream에서 RSS 내용을 파싱하여 RssFeed 객체로 변환
     */
    private RssFeed parseRssContentFromStream(InputStream inputStream, String rssUrl) {
        try {
            // Read the content first to debug and clean it
            byte[] content = inputStream.readAllBytes();

            // Check if content is gzip compressed (starts with magic bytes 0x1f 0x8b)
            boolean isGzipped = content.length >= 2 &&
                               (content[0] & 0xFF) == 0x1F &&
                               (content[1] & 0xFF) == 0x8B;

            String contentStr;
            if (isGzipped) {
                log.debug("Detected gzip compressed content from {}, decompressing...", rssUrl);
                try (GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(content))) {
                    byte[] decompressed = gzipIn.readAllBytes();
                    contentStr = new String(decompressed, "UTF-8");
                }
            } else {
                contentStr = new String(content, "UTF-8");
            }

            // Log first 500 characters for debugging
            log.debug("RSS content from {} ({}): {}", rssUrl,
                isGzipped ? "decompressed" : "raw",
                contentStr.length() > 500 ? contentStr.substring(0, 500) + "..." : contentStr);

            // Remove BOM if present
            if (contentStr.startsWith("\uFEFF")) {
                contentStr = contentStr.substring(1);
                log.debug("Removed BOM from RSS content");
            }

            // Check if content looks like XML
            String trimmedContent = contentStr.trim();
            if (!trimmedContent.startsWith("<?xml") && !trimmedContent.startsWith("<")) {
                throw new RssFetchException("Response doesn't appear to be XML content from " + rssUrl + ": " +
                    (contentStr.length() > 200 ? contentStr.substring(0, 200) + "..." : contentStr));
            }

            // Create new input stream from cleaned content
            try (XmlReader xmlReader = new XmlReader(new ByteArrayInputStream(contentStr.getBytes("UTF-8")))) {
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed syndFeed = input.build(xmlReader);

                List<RssEntry> entries = new ArrayList<>();

                if (syndFeed.getEntries() != null) {
                    for (SyndEntry syndEntry : syndFeed.getEntries()) {
                        try {
                            RssEntry entry = convertToRssEntry(syndEntry);
                            if (entry.isValid()) {
                                entries.add(entry);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to convert RSS entry: {} - {}", syndEntry.getTitle(), e.getMessage());
                        }
                    }
                }

                LocalDateTime lastBuildDate = convertToLocalDateTime(syndFeed.getPublishedDate());

                RssFeed rssFeed = RssFeed.builder()
                        .url(rssUrl)
                        .title(cleanText(syndFeed.getTitle()))
                        .description(cleanText(syndFeed.getDescription()))
                        .lastBuildDate(lastBuildDate)
                        .entries(entries)
                        .build();

                log.debug("Successfully parsed RSS feed from {}: {} entries", rssUrl, entries.size());
                return rssFeed;
            }

        } catch (Exception e) {
            String errorMsg = String.format("Failed to parse RSS content from [%s]: %s", rssUrl, e.getMessage());
            log.error(errorMsg, e);
            throw new RssFetchException(errorMsg, e);
        }
    }

    /**
     * SyndEntry를 RssEntry로 변환
     */
    private RssEntry convertToRssEntry(SyndEntry syndEntry) {
        String title = cleanText(syndEntry.getTitle());
        String content = extractContent(syndEntry);
        String url = syndEntry.getLink();
        String author = syndEntry.getAuthor();
        LocalDateTime publishedAt = convertToLocalDateTime(syndEntry.getPublishedDate());

        return RssEntry.builder()
                .title(title)
                .content(content)
                .url(url)
                .author(author)
                .publishedAt(publishedAt)
                .build();
    }

    /**
     * SyndEntry에서 콘텐츠 추출
     */
    private String extractContent(SyndEntry syndEntry) {
        String content = null;

        // Description 시도
        if (syndEntry.getDescription() != null) {
            content = syndEntry.getDescription().getValue();
        }

        // Contents 리스트에서 시도
        if ((content == null || content.trim().isEmpty()) &&
                syndEntry.getContents() != null && !syndEntry.getContents().isEmpty()) {
            content = syndEntry.getContents().get(0).getValue();
        }

        return cleanText(content);
    }

    /**
     * Date를 LocalDateTime으로 변환
     */
    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 텍스트 정리 (HTML 태그 제거, 공백 정리)
     */
    private String cleanText(String text) {
        if (text == null) {
            return null;
        }

        // HTML 태그 제거
        String cleaned = text.replaceAll("<[^>]*>", "");

        // 연속된 공백을 하나로 변경
        cleaned = cleaned.replaceAll("\\s+", " ");

        return cleaned.trim();
    }
}