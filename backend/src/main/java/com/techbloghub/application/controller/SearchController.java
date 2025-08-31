package com.techbloghub.application.controller;

import com.techbloghub.application.dto.PostResponse;
import com.techbloghub.application.dto.SearchRequest;
import com.techbloghub.domain.port.in.SearchUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 포스트 검색 API 컨트롤러
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search", description = "포스트 검색 API")
public class SearchController {

    private final SearchUseCase searchUseCase;

    @GetMapping("/posts")
    @Operation(summary = "포스트 검색", description = "다양한 조건으로 포스트를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = PostResponse.class)))
    })
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @ParameterObject SearchRequest searchRequest,
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<PostResponse> posts = searchUseCase.searchPosts(searchRequest.toSearchCondition(), pageable)
                .map(PostResponse::from);

        return ResponseEntity.ok(posts);
    }

}