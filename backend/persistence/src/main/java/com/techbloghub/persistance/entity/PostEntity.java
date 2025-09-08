package com.techbloghub.persistance.entity;

import com.techbloghub.domain.model.Post;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PostTagEntity> postTags = new HashSet<>();

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PostCategoryEntity> postCategories = new HashSet<>();

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
                .tags(postTags != null ? 
                    postTags.stream()
                        .filter(pt -> pt.getTag() != null)
                        .map(pt -> pt.getTag().toDomain())
                        .collect(Collectors.toSet()) : Set.of())
                .categories(postCategories != null ? 
                    postCategories.stream()
                        .filter(pc -> pc.getCategory() != null)
                        .map(pc -> pc.getCategory().toDomain())
                        .collect(Collectors.toSet()) : Set.of())
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
                // blog, tags, categories는 별도로 설정해야 함 (Entity 변환 필요)
                .build();
    }
    
    public static PostEntity fromDomainWithRelations(Post post, BlogEntity blogEntity, 
                                                   Set<TagEntity> tagEntities, 
                                                   Set<CategoryEntity> categoryEntities) {
        PostEntity postEntity = PostEntity.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .originalUrl(post.getOriginalUrl())
                .author(post.getAuthor())
                .publishedAt(post.getPublishedAt())
                .blog(blogEntity)
                .build();

        // PostTag 관계 생성
        Set<PostTagEntity> postTags = tagEntities.stream()
                .map(tagEntity -> PostTagEntity.create(postEntity, tagEntity))
                .collect(Collectors.toSet());
        postEntity.postTags = postTags;

        // PostCategory 관계 생성
        Set<PostCategoryEntity> postCategories = categoryEntities.stream()
                .map(categoryEntity -> PostCategoryEntity.create(postEntity, categoryEntity))
                .collect(Collectors.toSet());
        postEntity.postCategories = postCategories;

        return postEntity;
    }
}