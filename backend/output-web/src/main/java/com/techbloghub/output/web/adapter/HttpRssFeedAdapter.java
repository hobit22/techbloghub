package com.techbloghub.output.web.adapter;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techbloghub.domain.model.RssEntry;
import com.techbloghub.domain.model.RssFeed;
import com.techbloghub.domain.port.out.FetchRssPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * HTTP를 통한 RSS 피드 조회 어댑터
 */
@Component
@Slf4j
public class HttpRssFeedAdapter implements FetchRssPort {

    private static final String USER_AGENT = "TechBlogHub-Crawler/1.0";
    private int timeoutSeconds = 30;

    private final WebClient webClient;

    public HttpRssFeedAdapter() {
        this.webClient = WebClient.builder()
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    @Override
    public void setTimeout(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        log.debug("RSS fetch timeout set to {} seconds", timeoutSeconds);
    }

    @Override
    public RssFeed fetchRssFeed(String rssUrl) {
        log.debug("Fetching RSS feed from: {}", rssUrl);

        try {
            // HTTP로 RSS 피드를 스트리밍으로 가져오기
            Flux<DataBuffer> dataBufferFlux = webClient.get()
                    .uri(rssUrl)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds));

            // DataBuffer들을 하나의 InputStream으로 결합
            InputStream inputStream = DataBufferUtils.join(dataBufferFlux)
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return new ByteArrayInputStream(bytes);
                    })
                    .cast(InputStream.class)
                    .block();

            if (inputStream == null) {
                throw new RssFetchException("Empty RSS content received from: " + rssUrl);
            }

            // Rome Tools로 RSS 파싱 (InputStream 직접 사용)
            return parseRssContentFromStream(inputStream, rssUrl);

        } catch (WebClientRequestException e) {
            String errorMsg = String.format("Request failed for RSS URL [%s]: %s", rssUrl, e.getMessage());
            log.error(errorMsg, e);
            throw new RssFetchException(errorMsg, e);

        } catch (WebClientResponseException e) {
            String errorMsg = String.format("HTTP error %d for RSS URL [%s]: %s",
                    e.getStatusCode().value(), rssUrl, e.getMessage());
            log.error(errorMsg, e);
            throw new RssFetchException(errorMsg, e);

        } catch (Exception e) {
            String errorMsg = String.format("Failed to fetch RSS feed from [%s]: %s", rssUrl, e.getMessage());
            log.error(errorMsg, e);
            throw new RssFetchException(errorMsg, e);
        }
    }

    /**
     * InputStream에서 RSS 내용을 파싱하여 RssFeed 객체로 변환
     */
    private RssFeed parseRssContentFromStream(InputStream inputStream, String rssUrl) {
        try (XmlReader xmlReader = new XmlReader(inputStream)) {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed syndFeed = input.build(xmlReader);

            List<RssEntry> entries = new ArrayList<>();

            if (syndFeed.getEntries() != null) {
                for (SyndEntry syndEntry : syndFeed.getEntries()) {
                    try {
                        RssEntry entry = convertToRssEntry(syndEntry);
                        log.info("entry: {}", entry);
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