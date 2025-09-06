package com.techbloghub.persistance.entity;

import com.techbloghub.domain.model.Post;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_post_published_at", columnList = "publishedAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(access = AccessLevel.PRIVATE)
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

    public Post toDomain() {
        return Post.builder()
                .id(id)
                .title(title)
                .content(content)
                .originalUrl(originalUrl)
                .author(author)
                .publishedAt(publishedAt)
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .blog(this.blog.toDomain())
                .build();
    }
    
    public static PostEntity fromDomain(Post post) {
        return PostEntity.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .originalUrl(post.getOriginalUrl())
                .author(post.getAuthor())
                .publishedAt(post.getPublishedAt())
                // blog은 별도로 설정해야 함 (BlogEntity 필요)
                .build();
    }
}