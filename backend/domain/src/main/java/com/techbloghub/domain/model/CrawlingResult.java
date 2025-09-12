package com.techbloghub.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 크롤링 결과 도메인 모델
 */
@Getter
@Builder
public class CrawlingResult {

    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int totalBlogs;
    private final int processedBlogs;
    private final int totalPostsSaved;
    private final List<BlogCrawlingResult> blogResults;
    private final List<CrawlingError> errors;

    public CrawlingResult(LocalDateTime startTime, LocalDateTime endTime,
                          int totalBlogs, int processedBlogs, int totalPostsSaved,
                          List<BlogCrawlingResult> blogResults, List<CrawlingError> errors) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalBlogs = totalBlogs;
        this.processedBlogs = processedBlogs;
        this.totalPostsSaved = totalPostsSaved;
        this.blogResults = blogResults != null ? blogResults : new ArrayList<>();
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    /**
     * 크롤링 실행 시간 (초)
     */
    public long getExecutionTimeInSeconds() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).getSeconds();
    }

    /**
     * 크롤링이 성공적으로 완료되었는지 확인
     */
    public boolean isSuccessful() {
        return processedBlogs > 0 && errors.isEmpty();
    }

    /**
     * 부분적으로 성공했는지 확인 (일부 블로그는 성공, 일부는 실패)
     */
    public boolean isPartiallySuccessful() {
        return processedBlogs > 0 && !errors.isEmpty();
    }

    /**
     * 블로그별 크롤링 결과
     */
    @Getter
    @Builder
    public static class BlogCrawlingResult {
        private final Long blogId;
        private final String blogName;
        private final int postsSaved;
        private final int totalPostsFound;
        private final LocalDateTime crawledAt;
    }

    /**
     * 크롤링 에러 정보
     */
    @Getter
    @Builder
    public static class CrawlingError {
        private final Long blogId;
        private final String blogName;
        private final String errorMessage;
        private final String errorType;
        private final LocalDateTime occurredAt;
    }
}