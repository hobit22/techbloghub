package com.techbloghub.controller;

import com.techbloghub.dto.BlogResponse;
import com.techbloghub.entity.Blog;
import com.techbloghub.repository.BlogRepository;
import com.techbloghub.repository.PostRepository;
import com.techbloghub.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BlogController {
    
    private final BlogRepository blogRepository;
    private final PostRepository postRepository;
    private final CrawlerService crawlerService;
    
    @GetMapping
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
    public ResponseEntity<BlogResponse> getBlog(@PathVariable Long id) {
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
    public ResponseEntity<String> crawlBlog(@PathVariable Long id) {
        try {
            crawlerService.crawlSpecificBlog(id);
            return ResponseEntity.ok("Crawling started for blog ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error starting crawl: " + e.getMessage());
        }
    }
    
    @PostMapping("/crawl-all")
    public ResponseEntity<String> crawlAllBlogs() {
        try {
            crawlerService.crawlAllFeeds();
            return ResponseEntity.ok("Crawling started for all active blogs");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error starting crawl: " + e.getMessage());
        }
    }
}