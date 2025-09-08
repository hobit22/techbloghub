package com.techbloghub.persistance.entity;

import com.techbloghub.domain.model.Tag;
import jakarta.persistence.*;
import lombok.*;

/**
 * 태그 JPA 엔티티
 * 도메인 모델과 데이터베이스 테이블 간의 매핑 담당
 */
@Entity
@Table(name = "tags", indexes = {
        @Index(name = "idx_tag_name", columnList = "name", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class TagEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * 도메인 모델로 변환
     */
    public Tag toDomain() {
        return Tag.builder()
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
    public static TagEntity fromDomain(Tag tag) {
        return TagEntity.builder()
                .name(tag.getName())
                .description(tag.getDescription())
                .build();
    }

    /**
     * 도메인 모델로부터 엔티티 업데이트 (기존)
     */
    public static TagEntity fromDomainWithId(Tag tag) {
        return TagEntity.builder()
                .id(tag.getId())
                .name(tag.getName())
                .description(tag.getDescription())
                .build();
    }
}