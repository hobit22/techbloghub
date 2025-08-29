package com.techbloghub.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "포스트 검색 요청")
public class SearchRequest {
    @Schema(description = "검색 키워드", example = "Spring Boot")
    private String query;

    @Schema(description = "회사 필터", example = "[\"네이버\", \"카카오\"]")
    private List<String> companies;

    @Schema(description = "태그 필터", example = "[\"Java\", \"Spring\"]")
    private List<String> tags;

    @Schema(description = "카테고리 필터", example = "[\"Backend\", \"Frontend\"]")
    private List<String> categories;

    @Schema(description = "정렬 기준", example = "publishedAt", defaultValue = "publishedAt")
    private String sortBy = "publishedAt";

    @Schema(description = "정렬 방향", example = "desc", allowableValues = {"asc", "desc"}, defaultValue = "desc")
    private String sortDirection = "desc";

    @Schema(description = "페이지 번호", example = "0", defaultValue = "0")
    private int page = 0;

    @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
    private int size = 20;
}