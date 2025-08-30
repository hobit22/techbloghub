package com.techbloghub.application.controller;

import com.techbloghub.application.dto.CategoryResponse;
import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.port.in.CategoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "categories", description = "카테고리 관리 API")
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    @GetMapping
    @Operation(summary = "카테고리 목록 조회", description = "모든 카테고리 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "카테고리 목록 조회 성공")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<Category> categories = categoryUseCase.getAllCategories();
        
        List<CategoryResponse> responses = categories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

}