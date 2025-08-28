package com.techbloghub.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private LocalDateTime publishedAt;
    
    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
    
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
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .blogId(post.getBlog().getId())
                .blogName(post.getBlog().getName())
                .company(post.getBlog().getCompany())
                .tags(post.getTags() != null ? 
                      post.getTags().stream().map(Tag::getName).collect(java.util.stream.Collectors.toSet()) : 
                      java.util.Collections.emptySet())
                .categories(post.getCategories() != null ? 
                           post.getCategories().stream().map(Category::getName).collect(java.util.stream.Collectors.toSet()) : 
                           java.util.Collections.emptySet())
                .build();
    }
}