package com.techbloghub.api.controller;

import com.techbloghub.api.dto.CategoryResponse;
import com.techbloghub.domain.port.in.CategoryUseCase;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 카테고리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Category", description = "카테고리 API")
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    @GetMapping
    @Operation(summary = "전체 카테고리 목록 조회", description = "시스템에 등록된 모든 카테고리 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    })
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryUseCase.getAllCategories()
                .stream()
                .map(CategoryResponse::from)
                .toList();
        
        return ResponseEntity.ok(categories);
    }
}