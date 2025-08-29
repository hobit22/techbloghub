package com.techbloghub.application.dto;

import com.techbloghub.persistance.entity.TagEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "태그 응답 정보")
public class TagsResponse {

    @Schema(description = "태그 ID", example = "1")
    private Long id;

    @Schema(description = "태그 이름", example = "Java")
    private String name;

    @Schema(description = "태그 설명", example = "Java 프로그래밍 언어")
    private String description;

    @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;

    public static TagsResponse from(TagEntity tagEntity) {
        return TagsResponse.builder()
                .id(tagEntity.getId())
                .name(tagEntity.getName())
                .description(tagEntity.getDescription())
                .createdAt(tagEntity.getCreatedAt())
                .updatedAt(tagEntity.getUpdatedAt())
                .build();
    }
}