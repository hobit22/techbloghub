package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.Tag;
import com.techbloghub.domain.port.in.PostUseCase;
import com.techbloghub.domain.port.out.CategoryRepositoryPort;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import com.techbloghub.domain.port.out.TagRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 포스트 관련 비즈니스 로직을 처리하는 애플리케이션 서비스
 * PostUseCase 인터페이스를 구현하여 도메인 비즈니스 로직을 수행
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostService implements PostUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final TagRepositoryPort tagRepositoryPort;

    @Override
    public Page<Post> getPosts(Pageable pageable) {
        log.debug("Fetching posts with pageable: {}", pageable);
        return postRepositoryPort.findAllOrderByPublishedAtDesc(pageable);
    }

    @Override
    public Optional<Post> getPostById(Long id) {
        log.debug("Fetching post by id: {}", id);
        return postRepositoryPort.findById(id);
    }

    @Override
    public Page<Post> getPostsByBlog(Long blogId, Pageable pageable) {
        log.debug("Fetching posts by blog id: {} with pageable: {}", blogId, pageable);
        return postRepositoryPort.findByBlogIdOrderByPublishedAtDesc(blogId, pageable);
    }

    @Override
    public Page<Post> getRecentPosts(Pageable pageable) {
        log.debug("Fetching recent posts with pageable: {}", pageable);
        // 7일 이내 포스트를 최신순으로 조회
        return postRepositoryPort.findAllOrderByPublishedAtDesc(pageable);
    }

    @Override
    @Transactional
    public Post savePost(Post post) {
        log.debug("Saving post: {}", post.getTitle());
        
        // 도메인 규칙 검증
        if (!post.isValid()) {
            throw new IllegalArgumentException("Invalid post data");
        }
        
        return postRepositoryPort.save(post);
    }

    @Override
    public boolean existsByOriginalUrl(String originalUrl) {
        log.debug("Checking if post exists by URL: {}", originalUrl);
        return postRepositoryPort.existsByOriginalUrl(originalUrl);
    }

    @Override
    @Transactional
    public Post savePostWithTagsAndCategories(Post post) {
        log.debug("Saving post with tags and categories: {}", post.getTitle());
        
        // 도메인 규칙 검증
        if (!post.isValid()) {
            throw new IllegalArgumentException("Invalid post data");
        }
        
        // 중복 체크
        if (existsByOriginalUrl(post.getOriginalUrl())) {
            log.debug("Post already exists with URL: {}", post.getOriginalUrl());
            return post; // 이미 존재하는 포스트는 그대로 반환
        }
        
        // 태그 및 카테고리 자동 추출
        String content = post.getTitle() + " " + (post.getContent() != null ? post.getContent() : "");
        
        Set<Tag> extractedTags = tagRepositoryPort.extractAndCreateTags(content);
        Set<Category> extractedCategories = categoryRepositoryPort.extractAndCreateCategories(content);
        
        // 기존 태그/카테고리와 병합
        Set<Tag> allTags = new HashSet<>(post.getTags() != null ? post.getTags() : Set.of());
        allTags.addAll(extractedTags);
        
        Set<Category> allCategories = new HashSet<>(post.getCategories() != null ? post.getCategories() : Set.of());
        allCategories.addAll(extractedCategories);
        
        // 태그와 카테고리가 포함된 새로운 포스트 생성
        Post postWithTagsAndCategories = Post.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .originalUrl(post.getOriginalUrl())
                .author(post.getAuthor())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .blog(post.getBlog())
                .tags(allTags)
                .categories(allCategories)
                .build();
        
        return postRepositoryPort.save(postWithTagsAndCategories);
    }
}