package com.techbloghub.dto;

import com.techbloghub.entity.Post;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String originalUrl;
    private String author;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private BlogInfo blog;
    private Set<String> tags;
    private Set<String> categories;
    
    @Data
    @Builder
    public static class BlogInfo {
        private Long id;
        private String name;
        private String company;
        private String siteUrl;
        private String logoUrl;
    }
    
    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .originalUrl(post.getOriginalUrl())
                .author(post.getAuthor())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .blog(BlogInfo.builder()
                        .id(post.getBlog().getId())
                        .name(post.getBlog().getName())
                        .company(post.getBlog().getCompany())
                        .siteUrl(post.getBlog().getSiteUrl())
                        .logoUrl(post.getBlog().getLogoUrl())
                        .build())
                .tags(post.getTags() != null ? 
                      post.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toSet()) :
                      Set.of())
                .categories(post.getCategories() != null ?
                           post.getCategories().stream().map(cat -> cat.getName()).collect(Collectors.toSet()) :
                           Set.of())
                .build();
    }
}