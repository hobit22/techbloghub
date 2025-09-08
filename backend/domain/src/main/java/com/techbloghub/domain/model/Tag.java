package com.techbloghub.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 태그 도메인 모델
 * 순수한 비즈니스 로직을 담고 있으며, 인프라스트럭처에 의존하지 않음
 * post_tags 중간 테이블을 알지 못함 (헥사고날 아키텍처 원칙)
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "name"})
public class Tag {
    
    private final Long id;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    
    /**
     * 태그가 유효한지 검증하는 도메인 규칙
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() && name.length() <= 50;
    }
    
    /**
     * 태그 이름 정규화 (소문자, 공백 제거)
     */
    public String getNormalizedName() {
        return name != null ? name.trim().toLowerCase() : null;
    }
    
    /**
     * 태그가 특정 키워드와 매치되는지 확인
     */
    public boolean matches(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        return getNormalizedName().contains(keyword.trim().toLowerCase());
    }
}