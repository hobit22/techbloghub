package com.techbloghub.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "어드민 포스트 수정 요청")
public class AdminPostUpdateRequest {

    @Schema(description = "포스트 제목", example = "Spring Boot 3.0 새로운 기능들 (수정됨)")
    private String title;

    @Schema(description = "포스트 내용", example = "Spring Boot 3.0에서 추가된 새로운 기능들을 자세히 살펴봅니다...")
    private String content;

    @Schema(description = "작성자", example = "홍길동")
    private String author;

    @Schema(description = "태그 목록", example = "[\"Java\", \"Spring Boot\", \"Backend\"]")
    private Set<String> tags;

    @Schema(description = "카테고리 목록", example = "[\"Backend\", \"Framework\"]")
    private Set<String> categories;

    @Schema(description = "포스트 상태", example = "PUBLISHED", allowableValues = {"DRAFT", "PUBLISHED", "ARCHIVED"})
    private String status;
}