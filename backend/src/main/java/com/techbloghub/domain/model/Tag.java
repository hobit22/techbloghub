package com.techbloghub.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 태그 도메인 모델
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Tag {
    
    private final Long id;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    
    /**
     * 태그명이 유효한지 검증
     */
    public boolean isValid() {
        return name != null && 
               !name.trim().isEmpty() && 
               name.length() <= 50;
    }
}