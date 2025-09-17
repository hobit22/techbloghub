package com.techbloghub.output.rss.parser;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techbloghub.domain.model.RssEntry;
import com.techbloghub.domain.model.RssFeed;
import com.techbloghub.domain.port.out.FetchRssPort.RssFetchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Component
@Slf4j
public class RssContentParser {

    public RssFeed parseRssContent(InputStream inputStream, String rssUrl) {
        try {
            String contentStr = readAndCleanContent(inputStream, rssUrl);
            return parseXmlContent(contentStr, rssUrl);

        } catch (Exception e) {
            String errorMsg = String.format("Failed to parse RSS content from [%s]: %s", rssUrl, e.getMessage());
            log.error(errorMsg, e);
            throw new RssFetchException(errorMsg, e);
        }
    }

    private String readAndCleanContent(InputStream inputStream, String rssUrl) throws Exception {
        byte[] content = inputStream.readAllBytes();

        // Check if content is gzip compressed
        boolean isGzipped = isGzipCompressed(content);

        String contentStr;
        if (isGzipped) {
            log.debug("Detected gzip compressed content from {}, decompressing...", rssUrl);
            contentStr = decompressGzip(content);
        } else {
            contentStr = new String(content, "UTF-8");
        }

        log.debug("RSS content from {} ({}): {}", rssUrl,
                isGzipped ? "decompressed" : "raw",
                contentStr.length() > 500 ? contentStr.substring(0, 500) + "..." : contentStr);

        return cleanContent(contentStr, rssUrl);
    }

    private boolean isGzipCompressed(byte[] content) {
        return content.length >= 2 &&
                (content[0] & 0xFF) == 0x1F &&
                (content[1] & 0xFF) == 0x8B;
    }

    private String decompressGzip(byte[] content) throws Exception {
        try (GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(content))) {
            byte[] decompressed = gzipIn.readAllBytes();
            return new String(decompressed, "UTF-8");
        }
    }

    private String cleanContent(String contentStr, String rssUrl) {
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

        return contentStr;
    }

    private RssFeed parseXmlContent(String contentStr, String rssUrl) throws Exception {
        try (XmlReader xmlReader = new XmlReader(new ByteArrayInputStream(contentStr.getBytes("UTF-8")))) {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed syndFeed = input.build(xmlReader);

            List<RssEntry> entries = convertEntries(syndFeed.getEntries());
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
    }

    private List<RssEntry> convertEntries(List<SyndEntry> syndEntries) {
        List<RssEntry> entries = new ArrayList<>();

        if (syndEntries != null) {
            for (SyndEntry syndEntry : syndEntries) {
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

        return entries;
    }

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

    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

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