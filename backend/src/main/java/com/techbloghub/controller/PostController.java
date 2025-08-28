package com.techbloghub.controller;

import com.techbloghub.dto.PostResponse;
import com.techbloghub.dto.SearchRequest;
import com.techbloghub.entity.Post;
import com.techbloghub.entity.PostDocument;
import com.techbloghub.repository.PostRepository;
import com.techbloghub.service.SearchService;
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
public class PostController {
    
    private final PostRepository postRepository;
    private final SearchService searchService;
    
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "publishedAt") String sortBy,
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
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return postRepository.findById(id)
                .map(post -> ResponseEntity.ok(PostResponse.from(post)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/blog/{blogId}")
    public ResponseEntity<Page<PostResponse>> getPostsByBlog(
            @PathVariable Long blogId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByBlogIdOrderByPublishedAtDesc(blogId, pageable);
        Page<PostResponse> response = posts.map(PostResponse::from);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/search")
    public ResponseEntity<Page<PostResponse>> searchPosts(@RequestBody SearchRequest searchRequest) {
        Page<PostDocument> searchResults = searchService.search(searchRequest);
        
        Page<PostResponse> response = searchResults.map(doc -> {
            Post post = postRepository.findById(doc.getPostId())
                    .orElse(null);
            return post != null ? PostResponse.from(post) : null;
        }).map(postResponse -> postResponse);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/recent")
    public ResponseEntity<Page<PostResponse>> getRecentPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Post> posts = postRepository.findAllOrderByPublishedAtDesc(pageable);
        Page<PostResponse> response = posts.map(PostResponse::from);
        
        return ResponseEntity.ok(response);
    }
}