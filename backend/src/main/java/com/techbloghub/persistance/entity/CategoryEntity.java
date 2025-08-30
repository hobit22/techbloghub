package com.techbloghub.persistance.entity;

import com.techbloghub.domain.model.Category;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class CategoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @Column
    private String color;

    public Category toDomain() {
        return Category.builder()
                .id(this.getId())
                .name(this.getName())
                .description(this.getDescription())
                .color(this.getColor())
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .build();
    }

    public static CategoryEntity from(Category domain) {
        return CategoryEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .color(domain.getColor())
                .build();
    }
}