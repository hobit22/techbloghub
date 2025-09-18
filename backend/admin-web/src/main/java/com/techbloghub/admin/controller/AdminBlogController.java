package com.techbloghub.admin.controller;

import com.techbloghub.admin.dto.AdminBlogCreateRequest;
import com.techbloghub.admin.dto.AdminBlogResponse;
import com.techbloghub.domain.blog.model.Blog;
import com.techbloghub.domain.crawling.model.CrawlingResult;
import com.techbloghub.domain.blog.usecase.BlogUseCase;
import com.techbloghub.domain.crawling.usecase.CrawlRssUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 어드민 블로그 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/blogs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Blogs", description = "어드민 블로그 관리 API")
public class AdminBlogController {

    private final BlogUseCase blogUseCase;
    private final CrawlRssUseCase crawlRssUseCase;

    @GetMapping
    @Operation(summary = "어드민 블로그 목록 조회", description = "관리자용 블로그 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "블로그 목록 조회 성공")
    public ResponseEntity<Page<AdminBlogResponse>> getAllBlogs(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Blog> blogs = blogUseCase.getAllBlogs(pageable);
        Page<AdminBlogResponse> adminBlogs = blogs.map(AdminBlogResponse::from);

        return ResponseEntity.ok(adminBlogs);
    }

    @GetMapping("/active")
    @Operation(summary = "활성 블로그 목록 조회", description = "활성화된 블로그 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "활성 블로그 목록 조회 성공")
    public ResponseEntity<List<AdminBlogResponse>> getActiveBlogs() {

        List<Blog> activeBlogs = blogUseCase.getActiveBlogs();
        List<AdminBlogResponse> adminBlogs = activeBlogs.stream()
                .map(AdminBlogResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(adminBlogs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "어드민 블로그 상세 조회", description = "관리자용 블로그 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "블로그 조회 성공"),
            @ApiResponse(responseCode = "404", description = "블로그를 찾을 수 없음")
    })
    public ResponseEntity<AdminBlogResponse> getBlog(
            @Parameter(description = "블로그 ID", example = "1")
            @PathVariable Long id) {

        Optional<Blog> blog = blogUseCase.getBlogById(id);
        if (blog.isPresent()) {
            AdminBlogResponse response = AdminBlogResponse.from(blog.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/recrawl")
    @Operation(summary = "블로그 재크롤링 트리거", description = "특정 블로그의 재크롤링을 요청합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재크롤링 요청 성공"),
            @ApiResponse(responseCode = "404", description = "블로그를 찾을 수 없음")
    })
    public ResponseEntity<String> triggerRecrawling(
            @Parameter(description = "블로그 ID", example = "1")
            @PathVariable Long id) {

        try {
            CrawlingResult crawlingResult = crawlRssUseCase.crawlSpecificBlog(id);
            return ResponseEntity.ok("재크롤링 요청이 성공적으로 전송되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("재크롤링 요청 중 오류 발생: ID={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/recrawl/all")
    @Operation(summary = "전체 블로그 재크롤링 트리거", description = "모든 활성 블로그의 재크롤링을 요청합니다.")
    @ApiResponse(responseCode = "200", description = "전체 재크롤링 요청 성공")
    public ResponseEntity<String> triggerAllRecrawling() {

        try {
            crawlRssUseCase.crawlAllActiveBlogs();
            return ResponseEntity.ok("전체 재크롤링 요청이 성공적으로 전송되었습니다.");
        } catch (Exception e) {
            log.error("전체 재크롤링 요청 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "블로그 통계 조회", description = "특정 블로그의 통계 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "블로그 통계 조회 성공"),
            @ApiResponse(responseCode = "404", description = "블로그를 찾을 수 없음")
    })
    public ResponseEntity<Object> getBlogStats(
            @Parameter(description = "블로그 ID", example = "1")
            @PathVariable Long id) {

        try {
            java.util.Map<String, Object> stats = blogUseCase.getBlogStats(id);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("블로그 통계 조회 중 오류 발생: ID={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @Operation(summary = "새로운 블로그 생성", description = "새로운 블로그를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "블로그 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ResponseEntity<AdminBlogResponse> createBlog(
            @Valid @RequestBody AdminBlogCreateRequest request) {

        try {
            Blog createdBlog = blogUseCase.createBlog(
                    request.getName(),
                    request.getCompany(),
                    request.getRssUrl(),
                    request.getSiteUrl(),
                    request.getLogoUrl(),
                    request.getDescription()
            );

            AdminBlogResponse response = AdminBlogResponse.from(createdBlog);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("블로그 생성 중 유효성 검사 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("블로그 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}