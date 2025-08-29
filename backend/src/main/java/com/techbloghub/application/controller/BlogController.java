package com.techbloghub.application.controller;

import com.techbloghub.application.dto.BlogResponse;
import com.techbloghub.domain.model.Blog;
import com.techbloghub.domain.port.in.BlogUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "blogs", description = "기술블로그 관리 API")
public class BlogController {

    private final BlogUseCase blogUseCase;

    @GetMapping
    @Operation(summary = "블로그 목록 조회", description = "활성화된 모든 기술블로그 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "블로그 목록 조회 성공")
    public ResponseEntity<List<BlogResponse>> getAllBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Blog> blogs = blogUseCase.getAllBlogs(pageable);
        
        List<BlogResponse> responses = blogs.getContent().stream()
                .map(BlogResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active")
    @Operation(summary = "활성 블로그 목록 조회", description = "활성화된 블로그 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "활성 블로그 목록 조회 성공")
    public ResponseEntity<List<BlogResponse>> getActiveBlogs() {
        List<Blog> blogs = blogUseCase.getActiveBlogs();
        
        List<BlogResponse> responses = blogs.stream()
                .map(BlogResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "블로그 상세 조회", description = "특정 블로그의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "블로그 조회 성공"),
            @ApiResponse(responseCode = "404", description = "블로그를 찾을 수 없음")
    })
    public ResponseEntity<BlogResponse> getBlog(
            @Parameter(description = "블로그 ID", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/company/{company}")
    @Operation(summary = "회사별 블로그 조회", description = "특정 회사의 블로그 목록을 조회합니다.")
    public ResponseEntity<List<BlogResponse>> getBlogsByCompany(@PathVariable String company) {
        return ResponseEntity.ok(List.of());
    }
}