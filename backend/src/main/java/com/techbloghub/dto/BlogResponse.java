package com.techbloghub.dto;

import com.techbloghub.entity.Blog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BlogResponse {
    private Long id;
    private String name;
    private String company;
    private String rssUrl;
    private String siteUrl;
    private String description;
    private String logoUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastCrawledAt;
    private Long postCount;
    
    public static BlogResponse from(Blog blog) {
        return BlogResponse.builder()
                .id(blog.getId())
                .name(blog.getName())
                .company(blog.getCompany())
                .rssUrl(blog.getRssUrl())
                .siteUrl(blog.getSiteUrl())
                .description(blog.getDescription())
                .logoUrl(blog.getLogoUrl())
                .status(blog.getStatus().name())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .lastCrawledAt(blog.getLastCrawledAt())
                .build();
    }
    
    public static BlogResponse from(Blog blog, Long postCount) {
        BlogResponse response = from(blog);
        response.setPostCount(postCount);
        return response;
    }
}