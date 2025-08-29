package com.techbloghub.controller;

import com.techbloghub.dto.PostResponse;
import com.techbloghub.dto.SearchRequest;
import com.techbloghub.entity.Post;
import com.techbloghub.repository.PostRepository;
import com.techbloghub.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "posts", description = "블로그 포스트 조회 및 검색 API")
public class PostController {

    private final PostRepository postRepository;
    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "포스트 목록 조회", description = "페이징된 포스트 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PostResponse.class)))
    })
    public ResponseEntity<Page<PostResponse>> getPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준 필드", example = "publishedAt")
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @Parameter(description = "정렬 방향 (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
        );

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Post> posts = postRepository.findAllOrderByPublishedAtDesc(pageable);
        Page<PostResponse> response = posts.map(PostResponse::from);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "포스트 상세 조회", description = "특정 포스트의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 조회 성공"),
            @ApiResponse(responseCode = "404", description = "포스트를 찾을 수 없음")
    })
    public ResponseEntity<PostResponse> getPost(
            @Parameter(description = "포스트 ID", required = true, example = "1")
            @PathVariable Long id) {
        return postRepository.findById(id)
                .map(post -> ResponseEntity.ok(PostResponse.from(post)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/blog/{blogId}")
    @Operation(summary = "특정 블로그의 포스트 목록 조회", description = "특정 블로그에 속한 포스트들을 조회합니다.")
    public ResponseEntity<Page<PostResponse>> getPostsByBlog(
            @Parameter(description = "블로그 ID", required = true, example = "1")
            @PathVariable Long blogId,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByBlogIdOrderByPublishedAtDesc(blogId, pageable);
        Page<PostResponse> response = posts.map(PostResponse::from);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    @Operation(summary = "포스트 검색", description = "다양한 조건으로 포스트를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공")
    })
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @Parameter(description = "검색 조건", required = true)
            @RequestBody SearchRequest searchRequest) {
        Page<Post> searchResults = searchService.search(searchRequest);
        Page<PostResponse> response = searchResults.map(PostResponse::from);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @Operation(summary = "최신 포스트 조회", description = "최신 포스트들을 조회합니다.")
    public ResponseEntity<Page<PostResponse>> getRecentPosts(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Post> posts = postRepository.findAllOrderByPublishedAtDesc(pageable);
        Page<PostResponse> response = posts.map(PostResponse::from);

        return ResponseEntity.ok(response);
    }
}