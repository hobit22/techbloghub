package com.techbloghub.persistance.entity;

import com.techbloghub.domain.model.Blog;
import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.Tag;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_post_published_at", columnList = "publishedAt")
})
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class PostEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, unique = true, length = 1023)
    private String originalUrl;

    @Column
    private String author;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private BlogEntity blog;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<PostTagEntity> postTags;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<PostCategoryEntity> postCategories;

    public Post toDomain() {
        Blog blog = Blog.builder()
                .id(this.getBlog().getId())
                .name(this.getBlog().getName())
                .company(this.getBlog().getCompany())
                .rssUrl(this.getBlog().getRssUrl())
                .siteUrl(this.getBlog().getSiteUrl())
                .description(this.getBlog().getDescription())
                .status(this.getBlog().getStatus())
                .lastCrawledAt(this.getBlog().getLastCrawledAt())
                .createdAt(this.getBlog().getCreatedAt())
                .updatedAt(this.getBlog().getUpdatedAt())
                .build();

        Set<Tag> tags = this.getPostTags() != null ?
                this.getPostTags().stream()
                        .map(postTag -> Tag.builder()
                                .id(postTag.getTag().getId())
                                .name(postTag.getTag().getName())
                                .createdAt(postTag.getTag().getCreatedAt())
                                .updatedAt(postTag.getTag().getUpdatedAt())
                                .build())
                        .collect(Collectors.toSet()) : Set.of();

        Set<Category> categories = this.getPostCategories() != null ?
                this.getPostCategories().stream()
                        .map(postCategory -> Category.builder()
                                .id(postCategory.getCategory().getId())
                                .name(postCategory.getCategory().getName())
                                .createdAt(postCategory.getCategory().getCreatedAt())
                                .updatedAt(postCategory.getCategory().getUpdatedAt())
                                .build())
                        .collect(Collectors.toSet()) : Set.of();

        return Post.builder()
                .id(this.getId())
                .title(this.getTitle())
                .content(this.getContent())
                .originalUrl(this.getOriginalUrl())
                .author(this.getAuthor())
                .publishedAt(this.getPublishedAt())
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .blog(blog)
                .tags(tags)
                .categories(categories)
                .build();
    }
}