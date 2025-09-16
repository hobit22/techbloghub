package com.techbloghub.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거부된 태그 엔티티
 * LLM이 제안했지만 미리 정의된 화이트리스트에 없어서 거부된 태그들을 추적
 */
@Entity
@Table(
    name = "rejected_tags",
    indexes = {
        @Index(name = "idx_rejected_tags_name", columnList = "tagName"),
        @Index(name = "idx_rejected_tags_frequency", columnList = "frequencyCount"),
        @Index(name = "idx_rejected_tags_status", columnList = "status"),
        @Index(name = "idx_rejected_tags_created_at", columnList = "createdAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RejectedTagEntity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String tagName;
    
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
    private RejectedStatus status = RejectedStatus.PENDING;
    
    @Column(length = 255)
    private String rejectionReason;
    
    public RejectedTagEntity(String tagName, PostEntity post) {
        this.tagName = tagName;
        this.post = post;
        this.postTitle = post.getTitle();
        this.postUrl = post.getOriginalUrl();
        this.blogName = post.getBlog() != null ? post.getBlog().getName() : null;
        this.frequencyCount = 1;
        this.status = RejectedStatus.PENDING;
    }
    
    public RejectedTagEntity(String tagName, PostEntity post, String rejectionReason) {
        this(tagName, post);
        this.rejectionReason = rejectionReason;
    }
    
    public void incrementFrequency() {
        this.frequencyCount++;
    }
    
    public void approve() {
        this.status = RejectedStatus.APPROVED;
    }
    
    public void ignore() {
        this.status = RejectedStatus.IGNORED;
    }
    
    public enum RejectedStatus {
        PENDING("대기 중"),
        APPROVED("승인됨"),
        IGNORED("무시됨");
        
        private final String description;
        
        RejectedStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}