package com.techbloghub.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 카테고리 도메인 모델
 * 순수한 비즈니스 로직을 담고 있으며, 인프라스트럭처에 의존하지 않음
 * post_categories 중간 테이블을 알지 못함 (헥사고날 아키텍처 원칙)
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "name"})
public class Category {
    
    private final Long id;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    
    /**
     * 카테고리가 유효한지 검증하는 도메인 규칙
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() && name.length() <= 100;
    }
    
    /**
     * 카테고리 이름 정규화 (트림, 대소문자 유지)
     */
    public String getNormalizedName() {
        return name != null ? name.trim() : null;
    }
    
    /**
     * 카테고리가 특정 키워드와 매치되는지 확인
     */
    public boolean matches(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        return getNormalizedName().toLowerCase().contains(keyword.trim().toLowerCase());
    }
    
    /**
     * 카테고리가 기술 관련 카테고리인지 확인하는 비즈니스 로직
     */
    public boolean isTechCategory() {
        if (name == null) return false;
        String normalizedName = getNormalizedName().toLowerCase();
        return normalizedName.contains("tech") || 
               normalizedName.contains("development") || 
               normalizedName.contains("programming") ||
               normalizedName.contains("backend") ||
               normalizedName.contains("frontend");
    }
}