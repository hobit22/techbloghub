package com.techbloghub.domain.model;

import lombok.*;

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
    private final String author;
    private final LocalDateTime publishedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    
    // 연관된 도메인 객체들
    private final Blog blog;
    private final Set<Tag> tags;
    private final Set<Category> categories;
    
    /**
     * 포스트가 유효한지 검증하는 도메인 규칙
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty() 
               && originalUrl != null && !originalUrl.trim().isEmpty()
               && blog != null;
    }
    
    /**
     * 포스트가 최신인지 확인 (7일 이내)
     */
    public boolean isRecent() {
        if (publishedAt == null) return false;
        return publishedAt.isAfter(LocalDateTime.now().minusDays(7));
    }
    
    /**
     * 특정 태그를 포함하는지 확인
     */
    public boolean hasTag(String tagName) {
        return tags != null && tags.stream()
                .anyMatch(tag -> tag.getName().equals(tagName));
    }
    
    /**
     * 특정 카테고리를 포함하는지 확인
     */
    public boolean hasCategory(String categoryName) {
        return categories != null && categories.stream()
                .anyMatch(category -> category.getName().equals(categoryName));
    }
}