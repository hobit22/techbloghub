package com.techbloghub.api.dto;

import com.techbloghub.domain.model.SearchCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "포스트 검색 조건")
public class SearchRequest {

    @Schema(description = "검색 키워드 (제목, 내용에서 검색)", example = "Spring Boot")
    private String keyword;

    @Schema(description = "태그 목록", example = "java, spring")
    private List<String> tags;

    @Schema(description = "카테고리 목록", example = "backend, devops")
    private List<String> categories;

    @Schema(description = "블로그 ID 목록", example = "1, 2, 3")
    private List<Long> blogIds;

    @Schema(description = "발행일 시작 범위", example = "2024-01-01")
    private LocalDate publishedAfter;

    @Schema(description = "발행일 종료 범위", example = "2024-12-31")
    private LocalDate publishedBefore;

    @Schema(description = "정렬 필드", example = "publishedAt", defaultValue = "publishedAt")
    private String sortBy = "publishedAt";

    @Schema(description = "정렬 방향 (asc, desc)", example = "desc", defaultValue = "desc")
    private String sortDirection = "desc";

    public SearchCondition toSearchCondition() {
        Sort.Direction direction = "asc".equalsIgnoreCase(this.sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return SearchCondition.builder()
                .keyword(this.keyword)
                .tags(this.tags)
                .categories(this.categories)
                .blogIds(this.blogIds)
                .publishedAfter(this.publishedAfter)
                .publishedBefore(this.publishedBefore)
                .sortBy(this.sortBy)
                .sortDirection(direction)
                .build();
    }
}