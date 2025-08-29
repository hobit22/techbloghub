package com.techbloghub.application.dto;

import com.techbloghub.persistance.entity.BlogEntity;
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

    public static BlogResponse from(BlogEntity blogEntity) {
        return BlogResponse.builder()
                .id(blogEntity.getId())
                .name(blogEntity.getName())
                .company(blogEntity.getCompany())
                .rssUrl(blogEntity.getRssUrl())
                .siteUrl(blogEntity.getSiteUrl())
                .description(blogEntity.getDescription())
                .logoUrl(blogEntity.getLogoUrl())
                .status(blogEntity.getStatus().name())
                .createdAt(blogEntity.getCreatedAt())
                .updatedAt(blogEntity.getUpdatedAt())
                .lastCrawledAt(blogEntity.getLastCrawledAt())
                .build();
    }

    public static BlogResponse from(BlogEntity blogEntity, Long postCount) {
        BlogResponse response = from(blogEntity);
        response.setPostCount(postCount);
        return response;
    }
}