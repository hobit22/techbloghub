package com.techbloghub.persistance.infrastructure;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techbloghub.domain.model.Blog;
import com.techbloghub.persistance.entity.BlogEntity;
import com.techbloghub.persistance.entity.PostEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * RSS 피드 크롤링을 담당하는 Infrastructure Layer 컴포넌트
 * 외부 RSS 라이브러리와 웹 클라이언트를 사용하는 기술적 구현 세부사항을 포함
 */
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

    public List<PostEntity> crawlFeed(BlogEntity blogEntity) {
        try {
            log.info("Crawling RSS feed for blog: {}", blogEntity.getName());

            String feedContent = webClient.get()
                    .uri(blogEntity.getRssUrl())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (feedContent == null) {
                log.warn("No content received from RSS feed: {}", blogEntity.getRssUrl());
                return new ArrayList<>();
            }

            SyndFeed feed = input.build(new XmlReader(new ByteArrayInputStream(feedContent.getBytes())));
            List<PostEntity> postEntities = new ArrayList<>();

            for (SyndEntry entry : feed.getEntries()) {
                try {
                    PostEntity postEntity = createPostFromEntry(entry, blogEntity);
                    if (postEntity != null) {
                        postEntities.add(postEntity);
                    }
                } catch (Exception e) {
                    log.error("Error processing entry from {}: {}", blogEntity.getName(), e.getMessage());
                }
            }

            log.info("Crawled {} posts from {}", postEntities.size(), blogEntity.getName());
            return postEntities;

        } catch (Exception e) {
            log.error("Error crawling RSS feed for blog {}: {}", blogEntity.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Domain Blog 객체를 받아서 크롤링하는 메서드
     */
    public List<PostEntity> crawlPostsFromBlog(Blog blog) {
        // Domain Blog를 Entity로 변환
        BlogEntity blogEntity = BlogEntity.builder()
                .id(blog.getId())
                .name(blog.getName())
                .company(blog.getCompany())
                .rssUrl(blog.getRssUrl())
                .siteUrl(blog.getSiteUrl())
                .description(blog.getDescription())
                .status(blog.getStatus())
                .lastCrawledAt(blog.getLastCrawledAt())
                .build();
        
        // 기존 크롤링 메서드 호출
        return crawlFeed(blogEntity);
    }

    private PostEntity createPostFromEntry(SyndEntry entry, BlogEntity blogEntity) {
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

            return PostEntity.builder()
                    .title(title)
                    .content(content)
                    .originalUrl(url)
                    .author(author)
                    .publishedAt(publishedAt)
                    .blog(blogEntity)
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