package com.techbloghub.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거부된 카테고리 엔티티  
 * LLM이 제안했지만 미리 정의된 화이트리스트에 없어서 거부된 카테고리들을 추적
 */
@Entity
@Table(
    name = "rejected_categories",
    indexes = {
        @Index(name = "idx_rejected_categories_name", columnList = "categoryName"),
        @Index(name = "idx_rejected_categories_frequency", columnList = "frequencyCount"),
        @Index(name = "idx_rejected_categories_status", columnList = "status"),
        @Index(name = "idx_rejected_categories_created_at", columnList = "createdAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RejectedCategoryEntity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String categoryName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;
    
    @Column(length = 500)
    private String postTitle;
    
    @Column(length = 1023)
    private String postUrl;
    
    @Column(length = 255)
    private String blogName;
    
    @Column(nullable = false)
    private Integer frequencyCount = 1;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RejectedTagEntity.RejectedStatus status = RejectedTagEntity.RejectedStatus.PENDING;
    
    @Column(length = 255)
    private String rejectionReason;
    
    public RejectedCategoryEntity(String categoryName, PostEntity post) {
        this.categoryName = categoryName;
        this.post = post;
        this.postTitle = post.getTitle();
        this.postUrl = post.getOriginalUrl();
        this.blogName = post.getBlog() != null ? post.getBlog().getName() : null;
        this.frequencyCount = 1;
        this.status = RejectedTagEntity.RejectedStatus.PENDING;
    }
    
    public RejectedCategoryEntity(String categoryName, PostEntity post, String rejectionReason) {
        this(categoryName, post);
        this.rejectionReason = rejectionReason;
    }
    
    public void incrementFrequency() {
        this.frequencyCount++;
    }
    
    public void approve() {
        this.status = RejectedTagEntity.RejectedStatus.APPROVED;
    }
    
    public void ignore() {
        this.status = RejectedTagEntity.RejectedStatus.IGNORED;
    }
}