package com.techbloghub.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Post와 Category 간의 중간 테이블 엔티티
 * @ManyToMany 대신 명시적인 중간 테이블을 사용하여 더 나은 제어와 확장성 제공
 */
@Entity
@Table(name = "post_categories", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "category_id"}),
       indexes = {
           @Index(name = "idx_post_category_post_id", columnList = "post_id"),
           @Index(name = "idx_post_category_category_id", columnList = "category_id")
       })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class PostCategoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private CategoryEntity category;

    /**
     * 정적 팩토리 메서드 - Post와 Category ID로 관계 생성
     */
    public static PostCategoryEntity create(Long postId, Long categoryId) {
        return PostCategoryEntity.builder()
                .postId(postId)
                .categoryId(categoryId)
                .build();
    }

    /**
     * 정적 팩토리 메서드 - Post와 Category 엔티티로 관계 생성
     */
    public static PostCategoryEntity create(PostEntity post, CategoryEntity category) {
        return PostCategoryEntity.builder()
                .postId(post.getId())
                .categoryId(category.getId())
                .post(post)
                .category(category)
                .build();
    }
}