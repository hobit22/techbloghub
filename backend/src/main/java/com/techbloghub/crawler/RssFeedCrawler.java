package com.techbloghub.crawler;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techbloghub.entity.Blog;
import com.techbloghub.entity.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RssFeedCrawler {

    private final WebClient webClient;
    private final SyndFeedInput input;

    public RssFeedCrawler() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        this.input = new SyndFeedInput();
    }

    public List<Post> crawlFeed(Blog blog) {
        try {
            log.info("Crawling RSS feed for blog: {}", blog.getName());

            String feedContent = webClient.get()
                    .uri(blog.getRssUrl())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (feedContent == null) {
                log.warn("No content received from RSS feed: {}", blog.getRssUrl());
                return new ArrayList<>();
            }

            SyndFeed feed = input.build(new XmlReader(new ByteArrayInputStream(feedContent.getBytes())));
            List<Post> posts = new ArrayList<>();

            for (SyndEntry entry : feed.getEntries()) {
                try {
                    Post post = createPostFromEntry(entry, blog);
                    if (post != null) {
                        posts.add(post);
                    }
                } catch (Exception e) {
                    log.error("Error processing entry from {}: {}", blog.getName(), e.getMessage());
                }
            }

            log.info("Crawled {} posts from {}", posts.size(), blog.getName());
            return posts;

        } catch (Exception e) {
            log.error("Error crawling RSS feed for blog {}: {}", blog.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private Post createPostFromEntry(SyndEntry entry, Blog blog) {
        try {
            String title = cleanTitle(entry.getTitle());
            String content = extractContent(entry);
            String url = entry.getLink();
            String author = extractAuthor(entry);
            LocalDateTime publishedAt = extractPublishedDate(entry);

            if (title == null || url == null) {
                log.warn("Skipping entry with missing title or URL");
                return null;
            }

            return Post.builder()
                    .title(title)
                    .content(content)
                    .originalUrl(url)
                    .author(author)
                    .publishedAt(publishedAt)
                    .blog(blog)
                    .build();

        } catch (Exception e) {
            log.error("Error creating post from entry: {}", e.getMessage());
            return null;
        }
    }

    private String cleanTitle(String title) {
        if (title == null) return null;
        return title.replaceAll("<[^>]*>", "").trim();
    }

    private String extractContent(SyndEntry entry) {
        String content = null;

        if (entry.getDescription() != null) {
            content = entry.getDescription().getValue();
        }

        if (content == null && !entry.getContents().isEmpty()) {
            content = entry.getContents().get(0).getValue();
        }

        if (content != null) {
            content = content.replaceAll("<[^>]*>", "").trim();
            if (content.length() > 1000) {
                content = content.substring(0, 1000) + "...";
            }
        }

        return content;
    }

    private String extractAuthor(SyndEntry entry) {
        if (entry.getAuthor() != null && !entry.getAuthor().trim().isEmpty()) {
            return entry.getAuthor().trim();
        }

        if (!entry.getAuthors().isEmpty()) {
            return entry.getAuthors().get(0).getName();
        }

        return null;
    }

    private LocalDateTime extractPublishedDate(SyndEntry entry) {
        if (entry.getPublishedDate() != null) {
            return LocalDateTime.ofInstant(
                    entry.getPublishedDate().toInstant(),
                    ZoneId.systemDefault()
            );
        }

        if (entry.getUpdatedDate() != null) {
            return LocalDateTime.ofInstant(
                    entry.getUpdatedDate().toInstant(),
                    ZoneId.systemDefault()
            );
        }

        return LocalDateTime.now();
    }
}