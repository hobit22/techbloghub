package com.techbloghub.persistance.adapter;

import com.techbloghub.domain.model.*;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import com.techbloghub.persistance.entity.*;
import com.techbloghub.persistance.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class PostAdapter implements PostRepositoryPort {
    
    private final PostRepository postRepository;

    @Override
    public Optional<Post> findById(Long id) {
        return postRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Post> findAllOrderByPublishedAtDesc(Pageable pageable) {
        return postRepository.findAllOrderByPublishedAtDesc(pageable)
                .map(this::toDomain);
    }

    @Override
    public Page<Post> findByBlogIdOrderByPublishedAtDesc(Long blogId, Pageable pageable) {
        return postRepository.findByBlogIdOrderByPublishedAtDesc(blogId, pageable)
                .map(this::toDomain);
    }

    @Override
    public List<Post> findRecentPosts(LocalDateTime since) {
        return postRepository.findRecentPosts(since)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Post> findByTagNames(List<String> tagNames, Pageable pageable) {
        return postRepository.findByTagNames(tagNames, pageable)
                .map(this::toDomain);
    }

    @Override
    public Page<Post> findByCategoryNames(List<String> categoryNames, Pageable pageable) {
        return postRepository.findByCategoryNames(categoryNames, pageable)
                .map(this::toDomain);
    }

    @Override
    public Page<Post> searchByKeyword(String keyword, Pageable pageable) {
        return Page.empty();
    }

    @Override
    public Page<Post> searchWithFilters(String keyword, List<String> companies,
                                       List<String> tags, List<String> categories,
                                       Pageable pageable) {
        return Page.empty();
    }

    @Override
    public boolean existsByOriginalUrl(String originalUrl) {
        return postRepository.existsByOriginalUrl(originalUrl);
    }

    @Override
    public long countByBlogId(Long blogId) {
        return postRepository.countByBlogId(blogId);
    }

    private Post toDomain(PostEntity entity) {
        Blog blog = Blog.builder()
                .id(entity.getBlog().getId())
                .name(entity.getBlog().getName())
                .company(entity.getBlog().getCompany())
                .rssUrl(entity.getBlog().getRssUrl())
                .siteUrl(entity.getBlog().getSiteUrl())
                .description(entity.getBlog().getDescription())
                .status(entity.getBlog().getStatus())
                .lastCrawledAt(entity.getBlog().getLastCrawledAt())
                .createdAt(entity.getBlog().getCreatedAt())
                .updatedAt(entity.getBlog().getUpdatedAt())
                .build();

        Set<Tag> tags = entity.getPostTags() != null ? 
                entity.getPostTags().stream()
                        .map(postTag -> Tag.builder()
                                .id(postTag.getTag().getId())
                                .name(postTag.getTag().getName())
                                .createdAt(postTag.getTag().getCreatedAt())
                                .updatedAt(postTag.getTag().getUpdatedAt())
                                .build())
                        .collect(Collectors.toSet()) : Set.of();

        Set<Category> categories = entity.getPostCategories() != null ?
                entity.getPostCategories().stream()
                        .map(postCategory -> Category.builder()
                                .id(postCategory.getCategory().getId())
                                .name(postCategory.getCategory().getName())
                                .createdAt(postCategory.getCategory().getCreatedAt())
                                .updatedAt(postCategory.getCategory().getUpdatedAt())
                                .build())
                        .collect(Collectors.toSet()) : Set.of();

        return Post.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .originalUrl(entity.getOriginalUrl())
                .author(entity.getAuthor())
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .blog(blog)
                .tags(tags)
                .categories(categories)
                .build();
    }

    private PostEntity toEntity(Post domain) {
        PostEntity entity = PostEntity.builder()
                .id(domain.getId())
                .title(domain.getTitle())
                .content(domain.getContent())
                .originalUrl(domain.getOriginalUrl())
                .author(domain.getAuthor())
                .publishedAt(domain.getPublishedAt())
                .build();

        if (domain.getBlog() != null) {
            BlogEntity blogEntity = BlogEntity.builder()
                    .id(domain.getBlog().getId())
                    .build();
            entity.setBlog(blogEntity);
        }

        return entity;
    }
}