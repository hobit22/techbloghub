package com.techbloghub.persistence.entity;

import com.techbloghub.domain.blog.model.Blog;
import com.techbloghub.domain.blog.model.BlogStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "blog")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class BlogEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false, unique = true)
    private String rssUrl;

    @Column(nullable = false)
    private String siteUrl;

    @Column
    private String description;

    @Column
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BlogStatus status = BlogStatus.ACTIVE;

    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;

    @Column(name = "failure_count")
    @Builder.Default
    private Integer failureCount = 0;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostEntity> postEntities;

    public static BlogEntity from(Blog domain) {
        return BlogEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .company(domain.getCompany())
                .rssUrl(domain.getRssUrl())
                .siteUrl(domain.getSiteUrl())
                .logoUrl(domain.getLogoUrl())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .lastCrawledAt(domain.getLastCrawledAt())
                .build();
    }

    public Blog toDomain() {
        return Blog.builder()
                .id(id)
                .name(name)
                .company(company)
                .rssUrl(rssUrl)
                .siteUrl(siteUrl)
                .logoUrl(logoUrl)
                .description(description)
                .status(status)
                .lastCrawledAt(lastCrawledAt)
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .build();
    }

    /**
     * 마지막 크롤링 시간 업데이트
     */
    public void updateLastCrawledAt(LocalDateTime lastCrawledAt) {
        this.lastCrawledAt = lastCrawledAt;
    }

    /**
     * 실패 횟수 증가
     */
    public void incrementFailureCount() {
        this.failureCount = (this.failureCount != null ? this.failureCount : 0) + 1;
    }

    /**
     * 실패 횟수 초기화
     */
    public void resetFailureCount() {
        this.failureCount = 0;
    }
}