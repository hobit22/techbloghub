package com.techbloghub.domain.service;

import com.techbloghub.domain.model.*;
import com.techbloghub.domain.port.in.CrawlRssUseCase;
import com.techbloghub.domain.port.out.BlogRepositoryPort;
import com.techbloghub.domain.port.out.FetchRssPort;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RSS 크롤링 도메인 서비스
 * 순수한 비즈니스 로직만을 담당하는 도메인 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RssCrawlerService implements CrawlRssUseCase {

    private static final int MAX_POSTS_PER_FEED = 100;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final BlogRepositoryPort blogRepositoryPort;
    private final FetchRssPort fetchRssPort;
    private final PostRepositoryPort postRepositoryPort;

    @Override
    public CrawlingResult crawlAllActiveBlogs() {
        log.info("Starting RSS crawling for all active blogs");

        LocalDateTime startTime = LocalDateTime.now();
        List<Blog> activeBlogs = blogRepositoryPort.findAll()
                .stream()
                .filter(Blog::isActive)
                .toList();

        CrawlingResult result = executeCrawling(activeBlogs, startTime);

        log.info("RSS crawling completed. Processed: {}/{}, Posts saved: {}, Duration: {}s",
                result.getProcessedBlogs(), result.getTotalBlogs(),
                result.getTotalPostsSaved(), result.getExecutionTimeInSeconds());

        return result;
    }

    @Override
    public CrawlingResult crawlSpecificBlog(Long blogId) {
        log.info("Starting RSS crawling for specific blog: {}", blogId);

        LocalDateTime startTime = LocalDateTime.now();
        Blog blog = blogRepositoryPort.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("Blog not found: " + blogId));

        CrawlingResult result = executeCrawling(List.of(blog), startTime);

        log.info("RSS crawling for blog {} completed. Posts saved: {}",
                blogId, result.getTotalPostsSaved());

        return result;
    }

    /**
     * 실제 크롤링 실행
     */
    private CrawlingResult executeCrawling(List<Blog> blogs, LocalDateTime startTime) {
        List<CrawlingResult.BlogCrawlingResult> blogResults = new ArrayList<>();
        List<CrawlingResult.CrawlingError> errors = new ArrayList<>();
        int totalPostsSaved = 0;

        fetchRssPort.setTimeout(DEFAULT_TIMEOUT_SECONDS);

        for (Blog blog : blogs) {
            try {
                if (!blog.isActive()) {
                    log.debug("Skipping inactive blog: {}", blog.getName());
                    continue;
                }

                if (!blog.hasValidRssUrl()) {
                    String errorMsg = "Invalid RSS URL for blog: " + blog.getName();
                    log.warn(errorMsg);
                    errors.add(createCrawlingError(blog, errorMsg, "INVALID_RSS_URL"));
                    continue;
                }

                CrawlingResult.BlogCrawlingResult blogResult = crawlSingleBlog(blog);
                blogResults.add(blogResult);
                totalPostsSaved += blogResult.getPostsSaved();

                // 성공 시 실패 카운트 초기화
                blogRepositoryPort.resetFailureCount(blog.getId());
                log.debug("Successfully crawled blog: {} ({} posts)",
                        blog.getName(), blogResult.getPostsSaved());

            } catch (Exception e) {
                String errorMsg = "Error crawling blog: " + blog.getName() + " - " + e.getMessage();
                log.error(errorMsg, e);

                errors.add(createCrawlingError(blog, errorMsg, e.getClass().getSimpleName()));
                blogRepositoryPort.incrementFailureCount(blog.getId());
            }
        }

        LocalDateTime endTime = LocalDateTime.now();

        return CrawlingResult.builder()
                .startTime(startTime)
                .endTime(endTime)
                .totalBlogs(blogs.size())
                .processedBlogs(blogResults.size())
                .totalPostsSaved(totalPostsSaved)
                .blogResults(blogResults)
                .errors(errors)
                .build();
    }

    /**
     * 단일 블로그 크롤링
     */
    private CrawlingResult.BlogCrawlingResult crawlSingleBlog(Blog blog) {
        LocalDateTime crawlStartTime = LocalDateTime.now();

        // RSS 피드 가져오기
        RssFeed rssFeed = fetchRssPort.fetchRssFeed(blog.getRssUrl());

        if (!rssFeed.isValid()) {
            throw new IllegalStateException("Invalid RSS feed received from: " + blog.getRssUrl());
        }

        // 유효한 엔트리들 가져오기 (최대 개수 제한)
        List<RssEntry> validEntries = rssFeed.getValidEntries(MAX_POSTS_PER_FEED);
        int totalPostsFound = validEntries.size();
        int postsSaved = 0;

        // 각 엔트리를 포스트로 저장
        for (RssEntry entry : validEntries) {
            try {
                Post post = entry.toPost(blog);

                // 사전 중복 체크
                if (postRepositoryPort.existsByNormalizedUrl(post.getNormalizedUrl())) {
                    log.debug("Duplicate post skipped: {}", entry.getUrl());
                    continue;
                }

                // 포스트 저장 (DB 제약조건이 최종 안전장치)
                postRepositoryPort.savePost(post);
                postsSaved++;

            } catch (Exception e) {
                log.warn("Failed to save post: {} from blog: {} - {}",
                        entry.getTitle(), blog.getName(), e.getMessage());
            }
        }

        // 블로그 마지막 크롤링 시간 업데이트
        blogRepositoryPort.updateLastCrawledAt(blog.getId(), crawlStartTime);

        return CrawlingResult.BlogCrawlingResult.builder()
                .blogId(blog.getId())
                .blogName(blog.getName())
                .postsSaved(postsSaved)
                .totalPostsFound(totalPostsFound)
                .crawledAt(crawlStartTime)
                .build();
    }

    /**
     * 크롤링 에러 객체 생성
     */
    private CrawlingResult.CrawlingError createCrawlingError(Blog blog, String errorMessage, String errorType) {
        return CrawlingResult.CrawlingError.builder()
                .blogId(blog.getId())
                .blogName(blog.getName())
                .errorMessage(errorMessage)
                .errorType(errorType)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}