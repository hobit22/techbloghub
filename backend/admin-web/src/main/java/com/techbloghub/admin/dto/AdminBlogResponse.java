package com.techbloghub.admin.dto;

import com.techbloghub.domain.model.Blog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "어드민 블로그 응답 정보")
public class AdminBlogResponse {
    
    @Schema(description = "블로그 ID", example = "1")
    private Long id;

    @Schema(description = "블로그 이름", example = "우아한형제들 기술블로그")
    private String name;

    @Schema(description = "회사명", example = "우아한형제들")
    private String company;

    @Schema(description = "RSS URL", example = "https://techblog.woowahan.com/feed")
    private String rssUrl;

    @Schema(description = "사이트 URL", example = "https://techblog.woowahan.com")
    private String siteUrl;

    @Schema(description = "블로그 설명", example = "우아한형제들의 기술 이야기")
    private String description;

    @Schema(description = "블로그 상태", example = "ACTIVE")
    private String status;

    @Schema(description = "마지막 크롤링 시간")
    private LocalDateTime lastCrawledAt;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;

    @Schema(description = "총 포스트 수", example = "145")
    private Long postCount;

    @Schema(description = "크롤링 성공 여부", example = "true")
    private Boolean lastCrawlSuccess;

    @Schema(description = "마지막 크롤링 오류 메시지")
    private String lastCrawlError;

    public static AdminBlogResponse from(Blog blog) {
        if (blog == null) {
            return null;
        }
        
        return AdminBlogResponse.builder()
                .id(blog.getId())
                .name(blog.getName())
                .company(blog.getCompany())
                .rssUrl(blog.getRssUrl())
                .siteUrl(blog.getSiteUrl())
                .description(blog.getDescription())
                .status(blog.getStatus() != null ? blog.getStatus().name() : null)
                .lastCrawledAt(blog.getLastCrawledAt())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                // TODO: postCount, lastCrawlSuccess, lastCrawlError는 추후 도메인 모델에 추가되면 매핑
                .build();
    }
}