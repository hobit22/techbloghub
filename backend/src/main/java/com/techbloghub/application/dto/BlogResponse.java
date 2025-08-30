package com.techbloghub.application.dto;

import com.techbloghub.domain.model.Blog;
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
                .status(blog.getStatus().name())
                .lastCrawledAt(blog.getLastCrawledAt())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }
}