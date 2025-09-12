package com.techbloghub.admin.controller;

import com.techbloghub.admin.dto.AdminBlogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 어드민 블로그 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/blogs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Blogs", description = "어드민 블로그 관리 API")
public class AdminBlogController {

    @GetMapping
    @Operation(summary = "어드민 블로그 목록 조회", description = "관리자용 블로그 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "블로그 목록 조회 성공")
    public ResponseEntity<Page<AdminBlogResponse>> getAllBlogs(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(null);
    }

    @GetMapping("/active")
    @Operation(summary = "활성 블로그 목록 조회", description = "활성화된 블로그 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "활성 블로그 목록 조회 성공")
    public ResponseEntity<List<AdminBlogResponse>> getActiveBlogs() {

        return ResponseEntity.ok(null);
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

        return ResponseEntity.ok(null);
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

        return ResponseEntity.ok("재크롤링 요청이 성공적으로 전송되었습니다.");
    }

    @PostMapping("/recrawl/all")
    @Operation(summary = "전체 블로그 재크롤링 트리거", description = "모든 활성 블로그의 재크롤링을 요청합니다.")
    @ApiResponse(responseCode = "200", description = "전체 재크롤링 요청 성공")
    public ResponseEntity<String> triggerAllRecrawling() {

        return ResponseEntity.ok(null);
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


        return ResponseEntity.ok("{\"message\": \"통계 데이터 준비 중\"}");
    }
}