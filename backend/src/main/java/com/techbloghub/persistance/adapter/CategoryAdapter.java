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
        CategoryEntity entity = CategoryEntity.from(category);
        CategoryEntity savedEntity = categoryRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id).map(CategoryEntity::toDomain);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name).map(CategoryEntity::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryEntity::toDomain)
                .collect(Collectors.toList());
    }
}