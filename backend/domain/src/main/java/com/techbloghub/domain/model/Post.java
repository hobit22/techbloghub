package com.techbloghub.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 포스트 도메인 모델
 * 순수한 비즈니스 로직을 담고 있으며, 인프라스트럭처에 의존하지 않음
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Post {

    private final Long id;
    private final String title;
    private final String content;
    private final String originalUrl;
    private final String normalizedUrl;
    private final String author;
    private final LocalDateTime publishedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // 연관된 도메인 객체
    private final Blog blog;
    private final Set<Tag> tags;
    private final Set<Category> categories;

    private TaggingProcessStatus taggingProcessStatus;

    /**
     * 포스트가 유효한지 검증하는 도메인 규칙
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty()
                && originalUrl != null && !originalUrl.trim().isEmpty()
                && normalizedUrl != null && !normalizedUrl.trim().isEmpty()
                && blog != null;
    }

    /**
     * 태그 이름 목록 반환
     */
    public Set<String> getTagNames() {
        if (tags == null) return Set.of();
        return tags.stream()
                .map(Tag::getName)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 카테고리 이름 목록 반환
     */
    public Set<String> getCategoryNames() {
        if (categories == null) return Set.of();
        return categories.stream()
                .map(Category::getName)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * URL을 정규화 (쿼리 파라미터, 프래그먼트 제거)
     */
    public static String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }

        String normalized = url.trim();

        // 쿼리 파라미터 제거
        int queryIndex = normalized.indexOf('?');
        if (queryIndex != -1) {
            normalized = normalized.substring(0, queryIndex);
        }

        // 프래그먼트 제거
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex != -1) {
            normalized = normalized.substring(0, fragmentIndex);
        }

        // 마지막 슬래시 제거
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized.toLowerCase();
    }

}