package com.techbloghub.admin.controller;

import com.techbloghub.admin.dto.AdminBatchDeleteRequest;
import com.techbloghub.admin.dto.AdminPostResponse;
import com.techbloghub.admin.dto.AdminPostUpdateRequest;
import com.techbloghub.domain.port.in.AdminPostUseCase;
import com.techbloghub.domain.port.in.SearchUseCase;
import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 어드민 포스트 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Posts", description = "어드민 포스트 관리 API")
public class AdminPostController {

    private final SearchUseCase searchUseCase;
    private final AdminPostUseCase adminPostUseCase;

    @GetMapping
    @Operation(summary = "어드민 포스트 목록 조회", description = "관리자용 포스트 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "포스트 목록 조회 성공")
    public ResponseEntity<Page<AdminPostResponse>> getAllPosts(
            @Parameter(description = "검색 키워드", example = "Spring")
            @RequestParam(required = false) String keyword,
            
            @Parameter(description = "블로그 ID", example = "1")
            @RequestParam(required = false) Long blogId,
            
            @Parameter(description = "태그", example = "Java")
            @RequestParam(required = false) String tag,
            
            @Parameter(description = "카테고리", example = "Backend")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        
        // SearchCondition 빌더를 사용하여 검색 조건 구성
        SearchCondition.SearchConditionBuilder conditionBuilder = SearchCondition.builder()
                .keyword(keyword);
        
        if (blogId != null) {
            conditionBuilder.blogIds(java.util.List.of(blogId));
        }
        if (tag != null) {
            conditionBuilder.tags(java.util.List.of(tag));
        }
        if (category != null) {
            conditionBuilder.categories(java.util.List.of(category));
        }
        
        SearchCondition searchCondition = conditionBuilder.build();
        
        Page<Post> posts = searchUseCase.searchPosts(searchCondition, pageable);
        Page<AdminPostResponse> adminPosts = posts.map(AdminPostResponse::from);

        return ResponseEntity.ok(adminPosts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "어드민 포스트 상세 조회", description = "관리자용 포스트 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 조회 성공"),
            @ApiResponse(responseCode = "404", description = "포스트를 찾을 수 없음")
    })
    public ResponseEntity<AdminPostResponse> getPost(
            @Parameter(description = "포스트 ID", example = "1")
            @PathVariable Long id) {
        
        Optional<Post> postOptional = adminPostUseCase.getPostForAdmin(id);
        
        if (postOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        AdminPostResponse response = AdminPostResponse.from(postOptional.get());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "어드민 포스트 수정", description = "포스트 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 수정 성공"),
            @ApiResponse(responseCode = "404", description = "포스트를 찾을 수 없음")
    })
    public ResponseEntity<AdminPostResponse> updatePost(
            @Parameter(description = "포스트 ID", example = "1")
            @PathVariable Long id,
            @RequestBody AdminPostUpdateRequest request) {
        
        try {
            Post updatedPost = adminPostUseCase.updatePost(id, request);
            AdminPostResponse response = AdminPostResponse.from(updatedPost);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("포스트 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "어드민 포스트 삭제", description = "포스트를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "포스트 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "포스트를 찾을 수 없음")
    })
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "포스트 ID", example = "1")
            @PathVariable Long id) {
        
        try {
            adminPostUseCase.deletePost(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("포스트 삭제 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/batch/delete")
    @Operation(summary = "어드민 포스트 일괄 삭제", description = "여러 포스트를 일괄 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "일괄 삭제 성공")
    public ResponseEntity<Void> deletePostsBatch(
            @RequestBody AdminBatchDeleteRequest request) {
        
        if (request.getIds() == null || request.getIds().isEmpty()) {
            log.warn("일괄 삭제 요청에 ID가 없음");
            return ResponseEntity.badRequest().build();
        }

        log.info("포스트 일괄 삭제 요청: IDs={}, 사유={}", request.getIds(), request.getReason());
        adminPostUseCase.deletePostsBatch(request.getIds());
        
        return ResponseEntity.noContent().build();
    }
}