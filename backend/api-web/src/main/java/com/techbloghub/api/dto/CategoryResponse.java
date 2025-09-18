package com.techbloghub.api.dto;

import com.techbloghub.domain.tagging.manual.model.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "카테고리 응답 정보")
public class CategoryResponse {
    
    @Schema(description = "카테고리 ID", example = "1")
    private Long id;
    
    @Schema(description = "카테고리 이름", example = "Backend")
    private String name;
    
    @Schema(description = "카테고리 설명", example = "백엔드 개발 관련 포스트")
    private String description;
    
    @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
    
    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}