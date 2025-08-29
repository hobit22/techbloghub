package com.techbloghub.persistance.adapter;

import com.techbloghub.domain.model.Blog;
import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.port.out.RssCrawlerPort;
import com.techbloghub.persistance.infrastructure.RssFeedCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RSS 크롤링을 위한 어댑터
 * 외부 크롤러 서비스와 도메인 계층을 연결하는 역할
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RssCrawlerAdapter implements RssCrawlerPort {

    private final RssFeedCrawler rssFeedCrawler;

    @Override
    public List<Post> crawlFeed(Blog blog) {
        log.debug("Crawling RSS feed for blog: {} ({})", blog.getName(), blog.getRssUrl());
        
        try {
            // RssFeedCrawler는 Entity를 반환하므로 Domain 객체로 변환 필요
            List<com.techbloghub.persistance.entity.PostEntity> postEntities = rssFeedCrawler.crawlPostsFromBlog(blog);
            
            return postEntities.stream()
                    .map(this::toDomain)
                    .collect(java.util.stream.Collectors.toList());
            
        } catch (Exception e) {
            log.error("Failed to crawl RSS feed for blog: {} ({})", 
                     blog.getName(), blog.getRssUrl(), e);
            return List.of();
        }
    }

    @Override
    public boolean isValidRssUrl(String rssUrl) {
        log.debug("Validating RSS URL: {}", rssUrl);
        
        if (rssUrl == null || rssUrl.trim().isEmpty()) {
            return false;
        }
        
        if (!rssUrl.startsWith("http://") && !rssUrl.startsWith("https://")) {
            return false;
        }
        
        try {
            // RSS URL에 대한 기본적인 검증
            // 실제로는 HTTP 요청을 보내서 XML 형식인지 확인할 수 있음
            return rssUrl.contains("rss") || 
                   rssUrl.contains("feed") || 
                   rssUrl.contains("atom") ||
                   rssUrl.endsWith(".xml");
                   
        } catch (Exception e) {
            log.warn("Error validating RSS URL: {}", rssUrl, e);
            return false;
        }
    }

    /**
     * PostEntity를 Domain Post로 변환
     */
    private Post toDomain(com.techbloghub.persistance.entity.PostEntity entity) {
        // Blog 정보 변환
        com.techbloghub.domain.model.Blog blog = com.techbloghub.domain.model.Blog.builder()
                .id(entity.getBlog().getId())
                .name(entity.getBlog().getName())
                .company(entity.getBlog().getCompany())
                .rssUrl(entity.getBlog().getRssUrl())
                .siteUrl(entity.getBlog().getSiteUrl())
                .description(entity.getBlog().getDescription())
                .status(entity.getBlog().getStatus())
                .lastCrawledAt(entity.getBlog().getLastCrawledAt())
                .createdAt(entity.getBlog().getCreatedAt())
                .updatedAt(entity.getBlog().getUpdatedAt())
                .build();

        return Post.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .originalUrl(entity.getOriginalUrl())
                .author(entity.getAuthor())
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .blog(blog)
                .tags(java.util.Set.of()) // 크롤링 단계에서는 빈 태그
                .categories(java.util.Set.of()) // 크롤링 단계에서는 빈 카테고리
                .build();
    }
}