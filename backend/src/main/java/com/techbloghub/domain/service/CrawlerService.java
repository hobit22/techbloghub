package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Blog;
import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.port.in.BlogUseCase;
import com.techbloghub.domain.port.in.CrawlerUseCase;
import com.techbloghub.domain.port.in.PostUseCase;
import com.techbloghub.domain.port.out.RssCrawlerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * RSS 크롤링 관련 비즈니스 로직을 처리하는 애플리케이션 서비스
 * CrawlerUseCase 인터페이스를 구현하여 크롤링 도메인 로직을 수행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService implements CrawlerUseCase {

    private final BlogUseCase blogUseCase;
    private final PostUseCase postUseCase;
    private final RssCrawlerPort rssCrawlerPort;

    @Override
    @Transactional
    public void crawlAllActiveBlogs() {
        log.info("Starting crawl for all active blogs");
        
        try {
            List<Blog> activeBlogs = blogUseCase.getActiveBlogs();
            log.info("Found {} active blogs to crawl", activeBlogs.size());
            
            int totalProcessed = 0;
            int totalSaved = 0;
            
            for (Blog blog : activeBlogs) {
                try {
                    int savedCount = crawlSpecificBlog(blog.getId());
                    totalSaved += savedCount;
                    totalProcessed++;
                    
                    log.debug("Crawled blog: {} ({}) - {} posts saved", 
                             blog.getName(), blog.getCompany(), savedCount);
                             
                } catch (Exception e) {
                    log.error("Failed to crawl blog: {} ({})", 
                             blog.getName(), blog.getRssUrl(), e);
                }
            }
            
            log.info("Crawling completed - {}/{} blogs processed, {} total posts saved", 
                    totalProcessed, activeBlogs.size(), totalSaved);
                    
        } catch (Exception e) {
            log.error("Error during crawl all active blogs", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public int crawlSpecificBlog(Long blogId) {
        log.debug("Starting crawl for blog ID: {}", blogId);
        
        try {
            // 블로그 정보 조회
            List<Blog> activeBlogs = blogUseCase.getActiveBlogs();
            Blog targetBlog = activeBlogs.stream()
                    .filter(blog -> blog.getId().equals(blogId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Active blog not found with ID: " + blogId));
            
            // RSS URL 유효성 검증
            if (!rssCrawlerPort.isValidRssUrl(targetBlog.getRssUrl())) {
                log.warn("Invalid RSS URL for blog: {} ({})", targetBlog.getName(), targetBlog.getRssUrl());
                return 0;
            }
            
            // RSS 피드 크롤링
            List<Post> crawledPosts = rssCrawlerPort.crawlFeed(targetBlog);
            log.debug("Crawled {} posts from blog: {}", crawledPosts.size(), targetBlog.getName());
            
            int savedCount = 0;
            for (Post post : crawledPosts) {
                try {
                    // 중복 체크 및 저장
                    if (!postUseCase.existsByOriginalUrl(post.getOriginalUrl())) {
                        postUseCase.savePostWithTagsAndCategories(post);
                        savedCount++;
                    } else {
                        log.debug("Post already exists, skipping: {}", post.getOriginalUrl());
                    }
                } catch (Exception e) {
                    log.error("Failed to save post: {} from blog: {}", 
                             post.getTitle(), targetBlog.getName(), e);
                }
            }
            
            log.debug("Saved {} new posts from blog: {}", savedCount, targetBlog.getName());
            return savedCount;
            
        } catch (Exception e) {
            log.error("Error crawling blog ID: {}", blogId, e);
            throw e;
        }
    }
}