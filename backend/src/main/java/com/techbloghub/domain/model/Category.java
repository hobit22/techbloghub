package com.techbloghub.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 카테고리 도메인 모델
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Category {
    
    private final Long id;
    private final String name;
    private final String description;
    private final String color;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    
    /**
     * 카테고리가 유효한지 검증
     */
    public boolean isValid() {
        return name != null && 
               !name.trim().isEmpty() && 
               name.length() <= 100;
    }
    
    /**
     * 색상 코드가 유효한지 검증
     */
    public boolean hasValidColor() {
        return color != null && 
               color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }


}