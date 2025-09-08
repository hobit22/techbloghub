package com.techbloghub.api.controller;

import com.techbloghub.api.dto.TagResponse;
import com.techbloghub.domain.port.in.TagUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 태그 API 컨트롤러
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tag", description = "태그 API")
public class TagController {

    private final TagUseCase tagUseCase;

    @GetMapping
    @Operation(summary = "전체 태그 목록 조회", description = "시스템에 등록된 모든 태그 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = TagResponse.class)))
    })
    public ResponseEntity<List<TagResponse>> getAllTags() {
        List<TagResponse> tags = tagUseCase.getAllTags()
                .stream()
                .map(TagResponse::from)
                .toList();
        
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/search")
    @Operation(summary = "태그 검색", description = "검색어에 해당하는 태그 목록을 조회합니다. 검색어가 없으면 전체 태그를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = TagResponse.class)))
    })
    public ResponseEntity<List<TagResponse>> searchTags(
            @RequestParam(value = "q", required = false) String query) {
        List<TagResponse> tags = tagUseCase.searchTags(query)
                .stream()
                .map(TagResponse::from)
                .toList();
        
        return ResponseEntity.ok(tags);
    }
}