package com.techbloghub.persistance.adapter;

import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.port.out.CategoryRepositoryPort;
import com.techbloghub.persistance.entity.CategoryEntity;
import com.techbloghub.persistance.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class CategoryAdapter implements CategoryRepositoryPort {
    
    private final CategoryRepository categoryRepository;
    
    @Override
    public Category save(Category category) {
        CategoryEntity entity = toEntity(category);
        CategoryEntity savedEntity = categoryRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name).map(this::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Category> extractAndCreateCategories(String text) {
        Set<Category> categories = new HashSet<>();
        
        if (text == null || text.trim().isEmpty()) {
            return categories;
        }
        
        Set<String> categoryNames = extractCategoryNames(text.toLowerCase());
        
        for (String categoryName : categoryNames) {
            Optional<Category> existingCategory = findByName(categoryName);
            if (existingCategory.isPresent()) {
                categories.add(existingCategory.get());
            } else {
                Category newCategory = Category.builder()
                        .name(categoryName)
                        .description(categoryName + " 카테고리")
                        .color(generateColor(categoryName))
                        .build();
                categories.add(save(newCategory));
            }
        }
        
        return categories;
    }

    private Set<String> extractCategoryNames(String text) {
        Set<String> categories = new HashSet<>();
        
        if (text.contains("java") || text.contains("spring") || text.contains("jvm")) {
            categories.add("Java");
        }
        if (text.contains("javascript") || text.contains("js") || text.contains("node") || 
            text.contains("react") || text.contains("vue") || text.contains("angular")) {
            categories.add("JavaScript");
        }
        if (text.contains("python") || text.contains("django") || text.contains("flask")) {
            categories.add("Python");
        }
        if (text.contains("데이터베이스") || text.contains("database") || text.contains("sql") || 
            text.contains("mysql") || text.contains("postgresql") || text.contains("mongodb")) {
            categories.add("Database");
        }
        if (text.contains("aws") || text.contains("azure") || text.contains("gcp") || 
            text.contains("cloud") || text.contains("클라우드")) {
            categories.add("Cloud");
        }
        if (text.contains("알고리즘") || text.contains("algorithm") || text.contains("자료구조")) {
            categories.add("Algorithm");
        }
        if (text.contains("머신러닝") || text.contains("machine learning") || text.contains("ai") ||
            text.contains("딥러닝") || text.contains("deep learning")) {
            categories.add("AI/ML");
        }
        if (text.contains("개발") || text.contains("development") || text.contains("프로그래밍")) {
            categories.add("Development");
        }
        
        return categories;
    }

    private String generateColor(String categoryName) {
        int hash = categoryName.hashCode();
        return String.format("#%06X", (hash & 0xFFFFFF));
    }

    private Category toDomain(CategoryEntity entity) {
        return Category.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .color(entity.getColor())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CategoryEntity toEntity(Category domain) {
        return CategoryEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .color(domain.getColor())
                .build();
    }
}