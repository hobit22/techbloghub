package com.techbloghub.controller;

import com.techbloghub.dto.BlogResponse;
import com.techbloghub.entity.Blog;
import com.techbloghub.repository.BlogRepository;
import com.techbloghub.repository.PostRepository;
import com.techbloghub.service.CrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    private final BlogRepository blogRepository;
    private final PostRepository postRepository;
    private final CrawlerService crawlerService;

    @GetMapping
    @Operation(summary = "블로그 목록 조회", description = "활성화된 모든 기술블로그 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "블로그 목록 조회 성공")
    public ResponseEntity<List<BlogResponse>> getAllBlogs() {
        List<Blog> blogs = blogRepository.findActiveBlogs();
        List<BlogResponse> response = blogs.stream()
                .map(blog -> {
                    Long postCount = postRepository.countByBlogId(blog.getId());
                    return BlogResponse.from(blog, postCount);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
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
        return blogRepository.findById(id)
                .map(blog -> {
                    Long postCount = postRepository.countByBlogId(blog.getId());
                    return ResponseEntity.ok(BlogResponse.from(blog, postCount));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/company/{company}")
    public ResponseEntity<List<BlogResponse>> getBlogsByCompany(@PathVariable String company) {
        List<Blog> blogs = blogRepository.findByCompanyContainingIgnoreCase(company);
        List<BlogResponse> response = blogs.stream()
                .map(blog -> {
                    Long postCount = postRepository.countByBlogId(blog.getId());
                    return BlogResponse.from(blog, postCount);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/crawl")
    @Operation(summary = "블로그 크롤링 시작", description = "특정 블로그의 RSS 피드를 크롤링합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "크롤링 시작 성공"),
            @ApiResponse(responseCode = "400", description = "크롤링 시작 실패")
    })
    public ResponseEntity<String> crawlBlog(
            @Parameter(description = "블로그 ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            crawlerService.crawlSpecificBlog(id);
            return ResponseEntity.ok("Crawling started for blog ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error starting crawl: " + e.getMessage());
        }
    }

    @PostMapping("/crawl-all")
    @Operation(summary = "전체 블로그 크롤링", description = "활성화된 모든 블로그의 RSS 피드를 크롤링합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전체 크롤링 시작 성공"),
            @ApiResponse(responseCode = "400", description = "크롤링 시작 실패")
    })
    public ResponseEntity<String> crawlAllBlogs() {
        try {
            crawlerService.crawlAllFeeds();
            return ResponseEntity.ok("Crawling started for all active blogs");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error starting crawl: " + e.getMessage());
        }
    }

}