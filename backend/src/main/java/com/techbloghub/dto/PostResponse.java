package com.techbloghub.dto;

import com.techbloghub.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@Schema(description = "블로그 포스트 응답 정보")
public class PostResponse {
    @Schema(description = "포스트 ID", example = "1")
    private Long id;

    @Schema(description = "포스트 제목", example = "Spring Boot 3.0 새로운 기능들")
    private String title;

    @Schema(description = "포스트 내용 요약", example = "Spring Boot 3.0에서 추가된 새로운 기능들을 살펴봅니다...")
    private String content;

    @Schema(description = "원본 포스트 URL", example = "https://d2.naver.com/helloworld/1234567")
    private String originalUrl;

    @Schema(description = "작성자", example = "홍길동")
    private String author;

    @Schema(description = "발행일시", example = "2024-01-15T10:30:00")
    private LocalDateTime publishedAt;

    @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "블로그 정보")
    private BlogInfo blog;

    @Schema(description = "태그 목록", example = "[\"Java\", \"Spring\", \"Backend\"]")
    private Set<String> tags;

    @Schema(description = "카테고리 목록", example = "[\"Backend\", \"Framework\"]")
    private Set<String> categories;

    @Data
    @Builder
    @Schema(description = "블로그 정보")
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
                .tags(post.getPostTags() != null ?
                        post.getPostTags().stream().map(postTag -> postTag.getTags().getName()).collect(Collectors.toSet()) :
                        Set.of())
                .categories(post.getPostCategories() != null ?
                        post.getPostCategories().stream().map(postCategory -> postCategory.getCategory().getName()).collect(Collectors.toSet()) :
                        Set.of())
                .build();
    }
}