package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.port.in.CategoryUseCase;
import com.techbloghub.domain.port.out.CategoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService implements CategoryUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;

    @Transactional
    public Category createCategory(String name, String description) {
        if (categoryRepositoryPort.existsByName(name)) {
            throw new IllegalArgumentException("Category with name '" + name + "' already exists");
        }
        
        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();
        
        return categoryRepositoryPort.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, String name, String description) {
        Optional<Category> existingCategory = categoryRepositoryPort.findById(id);
        if (existingCategory.isEmpty()) {
            throw new IllegalArgumentException("Category with id " + id + " not found");
        }

        Category category = Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .createdAt(existingCategory.get().getCreatedAt())
                .updatedAt(existingCategory.get().getUpdatedAt())
                .build();

        return categoryRepositoryPort.save(category);
    }

    public Optional<Category> findById(Long id) {
        return categoryRepositoryPort.findById(id);
    }

    public Optional<Category> findByName(String name) {
        return categoryRepositoryPort.findByName(name);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepositoryPort.findAll();
    }
    
    public List<Category> findAll() {
        return getAllCategories();
    }

    public List<Category> searchCategories(String keyword) {
        return categoryRepositoryPort.findByNameContaining(keyword);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepositoryPort.findById(id).isPresent()) {
            throw new IllegalArgumentException("Category with id " + id + " not found");
        }
        categoryRepositoryPort.deleteById(id);
    }

    public List<Category> findUnusedCategories() {
        return categoryRepositoryPort.findUnusedCategories();
    }

    public List<Category> findTechCategories() {
        return categoryRepositoryPort.findTechCategories();
    }

    @Transactional
    public Set<Category> getOrCreateCategories(Set<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            return Set.of();
        }

        Set<Category> result = new HashSet<>();
        List<Category> existingCategories = categoryRepositoryPort.findByNamesIn(categoryNames);
        
        Set<String> existingCategoryNames = new HashSet<>();
        for (Category existingCategory : existingCategories) {
            result.add(existingCategory);
            existingCategoryNames.add(existingCategory.getName());
        }

        for (String categoryName : categoryNames) {
            if (!existingCategoryNames.contains(categoryName)) {
                Category newCategory = Category.builder()
                        .name(categoryName)
                        .description("Auto-generated category")
                        .build();
                Category savedCategory = categoryRepositoryPort.save(newCategory);
                result.add(savedCategory);
            }
        }

        return result;
    }
}