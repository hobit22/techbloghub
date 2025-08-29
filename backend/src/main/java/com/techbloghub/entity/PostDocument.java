package com.techbloghub.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.Set;

@Document(indexName = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long postId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword)
    private String originalUrl;

    @Field(type = FieldType.Text)
    private String author;

    @Field(type = FieldType.Date)
    private LocalDate publishedAt;

    @Field(type = FieldType.Date)
    private LocalDate createdAt;

    @Field(type = FieldType.Long)
    private Long blogId;

    @Field(type = FieldType.Keyword)
    private String blogName;

    @Field(type = FieldType.Keyword)
    private String company;

    @Field(type = FieldType.Keyword)
    private Set<String> tags;

    @Field(type = FieldType.Keyword)
    private Set<String> categories;

    public static PostDocument fromPost(Post post) {
        return PostDocument.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .originalUrl(post.getOriginalUrl())
                .author(post.getAuthor())
                .publishedAt(LocalDate.from(post.getPublishedAt()))
                .createdAt(LocalDate.from(post.getCreatedAt()))
                .blogId(post.getBlog().getId())
                .blogName(post.getBlog().getName())
                .company(post.getBlog().getCompany())
                .tags(post.getPostTags() != null ?
                        post.getPostTags().stream().map(pt -> pt.getTags().getName()).collect(java.util.stream.Collectors.toSet()) :
                        java.util.Collections.emptySet())
                .categories(post.getPostCategories() != null ?
                        post.getPostCategories().stream().map(pc -> pc.getCategory().getName()).collect(java.util.stream.Collectors.toSet()) :
                        java.util.Collections.emptySet())
                .build();
    }
}