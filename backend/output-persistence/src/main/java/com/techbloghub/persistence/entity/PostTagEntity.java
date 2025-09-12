package com.techbloghub.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Post와 Tag 간의 중간 테이블 엔티티
 */
@Entity
@Table(name = "post_tags", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag_id"}),
       indexes = {
           @Index(name = "idx_post_tag_post_id", columnList = "post_id"),
           @Index(name = "idx_post_tag_tag_id", columnList = "tag_id")
       })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class PostTagEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    private TagEntity tag;

    /**
     * 정적 팩토리 메서드 - Post와 Tag ID로 관계 생성
     */
    public static PostTagEntity create(Long postId, Long tagId) {
        return PostTagEntity.builder()
                .postId(postId)
                .tagId(tagId)
                .build();
    }

    /**
     * 정적 팩토리 메서드 - Post와 Tag 엔티티로 관계 생성
     */
    public static PostTagEntity create(PostEntity post, TagEntity tag) {
        return PostTagEntity.builder()
                .postId(post.getId())
                .tagId(tag.getId())
                .post(post)
                .tag(tag)
                .build();
    }
}