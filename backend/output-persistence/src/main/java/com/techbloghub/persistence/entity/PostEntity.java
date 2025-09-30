package com.techbloghub.persistence.entity;

import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.auto.model.TaggingProcessStatus;
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
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "PostEntity.withAllRelations",
                attributeNodes = {
                        @NamedAttributeNode("blog"),
                        @NamedAttributeNode(value = "postTags", subgraph = "postTags"),
                        @NamedAttributeNode(value = "postCategories", subgraph = "postCategories")
                },
                subgraphs = {
                        @NamedSubgraph(name = "postTags", attributeNodes = @NamedAttributeNode("tag")),
                        @NamedSubgraph(name = "postCategories", attributeNodes = @NamedAttributeNode("category"))
                }
        ),
        @NamedEntityGraph(
                name = "PostEntity.withBlog",
                attributeNodes = @NamedAttributeNode("blog")
        )
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

    @Column(nullable = false, length = 1023)
    private String originalUrl;

    @Column(name = "normalized_url", length = 1023)
    @Builder.Default
    private String normalizedUrl = "";

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

    @Enumerated(EnumType.STRING)
    @Column(name = "tagging_process_status", nullable = false, columnDefinition = "VARCHAR DEFAULT 'NOT_PROCESSED'")
    @Builder.Default
    private TaggingProcessStatus taggingProcessStatus = TaggingProcessStatus.NOT_PROCESSED;

    @Column(name = "total_content", columnDefinition = "TEXT")
    private String totalContent;

    @Column(name = "summary_content", columnDefinition = "TEXT")
    private String summaryContent;

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
                .taggingProcessStatus(taggingProcessStatus)
                .totalContent(totalContent)
                .summaryContent(summaryContent)
                .build();
    }
    
    public static PostEntity fromDomain(Post post) {
        BlogEntity blogEntity = null;
        if (post.getBlog() != null) {
            blogEntity = BlogEntity.from(post.getBlog());
        }
        
        return PostEntity.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .originalUrl(post.getOriginalUrl())
                .normalizedUrl(post.getNormalizedUrl())
                .author(post.getAuthor())
                .publishedAt(post.getPublishedAt())
                .taggingProcessStatus(post.getTaggingProcessStatus() != null ? post.getTaggingProcessStatus() : TaggingProcessStatus.NOT_PROCESSED)
                .totalContent(post.getTotalContent())
                .summaryContent(post.getSummaryContent())
                .blog(blogEntity)
                .build();
    }
}