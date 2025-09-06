package com.techbloghub.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 블로그 도메인 모델
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Blog {

    private final Long id;
    private final String name;
    private final String company;
    private final String rssUrl;
    private final String siteUrl;
    private final String description;
    private final BlogStatus status;
    private final LocalDateTime lastCrawledAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * 블로그가 활성 상태인지 확인
     */
    public boolean isActive() {
        return status == BlogStatus.ACTIVE;
    }

    /**
     * 크롤링이 필요한지 확인 (마지막 크롤링으로부터 1시간 이상 경과)
     */
    public boolean needsCrawling() {
        if (lastCrawledAt == null) return true;
        return lastCrawledAt.isBefore(LocalDateTime.now().minusHours(1));
    }

    /**
     * RSS URL이 유효한지 검증
     */
    public boolean hasValidRssUrl() {
        return rssUrl != null &&
                !rssUrl.trim().isEmpty() &&
                (rssUrl.startsWith("http://") || rssUrl.startsWith("https://"));
    }
}