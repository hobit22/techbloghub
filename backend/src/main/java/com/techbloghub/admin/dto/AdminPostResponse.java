package com.techbloghub.admin.dto;

import com.techbloghub.domain.model.Blog;
import com.techbloghub.domain.model.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@Schema(description = "어드민 포스트 응답 정보")
public class AdminPostResponse {
    
    @Schema(description = "포스트 ID", example = "1")
    private Long id;

    @Schema(description = "포스트 제목", example = "Spring Boot 3.0 새로운 기능들")
    private String title;

    @Schema(description = "포스트 내용", example = "Spring Boot 3.0에서 추가된 새로운 기능들을 살펴봅니다...")
    private String content;

    @Schema(description = "원본 포스트 URL", example = "https://d2.naver.com/helloworld/1234567")
    private String originalUrl;

    @Schema(description = "작성자", example = "홍길동")
    private String author;

    @Schema(description = "발행일시", example = "2024-01-15T10:30:00")
    private LocalDateTime publishedAt;

    @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "블로그 정보")
    private AdminBlogInfo blog;

    @Schema(description = "태그 목록", example = "[\"Java\", \"Spring\", \"Backend\"]")
    private Set<String> tags;

    @Schema(description = "카테고리 목록", example = "[\"Backend\", \"Framework\"]")
    private Set<String> categories;

    @Schema(description = "포스트 상태", example = "PUBLISHED")
    private String status;

    @Schema(description = "조회수", example = "1234")
    private Long viewCount;

    @Data
    @Builder
    @Schema(description = "어드민 블로그 정보")
    public static class AdminBlogInfo {
        
        @Schema(description = "블로그 ID", example = "1")
        private Long id;
        
        @Schema(description = "블로그 이름", example = "우아한형제들 기술블로그")
        private String name;
        
        @Schema(description = "회사명", example = "우아한형제들")
        private String company;
        
        @Schema(description = "사이트 URL", example = "https://techblog.woowahan.com")
        private String siteUrl;
        
        @Schema(description = "RSS URL", example = "https://techblog.woowahan.com/feed")
        private String rssUrl;
        
        @Schema(description = "블로그 상태", example = "ACTIVE")
        private String status;
        
        @Schema(description = "마지막 크롤링 시간")
        private LocalDateTime lastCrawledAt;

        public static AdminBlogInfo from(Blog blog) {
            if (blog == null) {
                return null;
            }
            
            return AdminBlogInfo.builder()
                    .id(blog.getId())
                    .name(blog.getName())
                    .company(blog.getCompany())
                    .siteUrl(blog.getSiteUrl())
                    .rssUrl(blog.getRssUrl())
                    .status(blog.getStatus() != null ? blog.getStatus().name() : null)
                    .lastCrawledAt(blog.getLastCrawledAt())
                    .build();
        }
    }

    public static AdminPostResponse from(Post post) {
        if (post == null) {
            return null;
        }
        
        return AdminPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .originalUrl(post.getOriginalUrl())
                .author(post.getAuthor())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .blog(AdminBlogInfo.from(post.getBlog()))
                // TODO: tags, categories, status, viewCount는 추후 도메인 모델에 추가되면 매핑
                .build();
    }
}