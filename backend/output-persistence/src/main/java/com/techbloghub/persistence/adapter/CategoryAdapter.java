package com.techbloghub.persistence.adapter;

import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.port.out.CategoryRepositoryPort;
import com.techbloghub.persistence.entity.CategoryEntity;
import com.techbloghub.persistence.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class CategoryAdapter implements CategoryRepositoryPort {

    private final CategoryRepository categoryRepository;

    @Override
    @Cacheable("categories")
    public List<Category> findAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name)
                .map(CategoryEntity::toDomain);
    }

    @Override
    @CacheEvict("categories")
    public Category save(Category category) {
        CategoryEntity entity = CategoryEntity.fromDomain(category);
        CategoryEntity savedEntity = categoryRepository.save(entity);
        return savedEntity.toDomain();
    }
}