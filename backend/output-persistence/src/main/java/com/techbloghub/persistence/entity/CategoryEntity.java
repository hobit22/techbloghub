package com.techbloghub.persistence.entity;

import com.techbloghub.domain.tagging.manual.model.Category;
import jakarta.persistence.*;
import lombok.*;

/**
 * 카테고리 JPA 엔티티
 * 도메인 모델과 데이터베이스 테이블 간의 매핑 담당
 */
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_name", columnList = "name", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class CategoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * 도메인 모델로 변환
     */
    public Category toDomain() {
        return Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 모델로부터 엔티티 생성 (신규)
     */
    public static CategoryEntity fromDomain(Category category) {
        return CategoryEntity.builder()
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    /**
     * 도메인 모델로부터 엔티티 업데이트 (기존)
     */
    public static CategoryEntity fromDomainWithId(Category category) {
        return CategoryEntity.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}