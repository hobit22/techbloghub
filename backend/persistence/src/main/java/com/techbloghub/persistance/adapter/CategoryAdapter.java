package com.techbloghub.persistance.adapter;

import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.port.out.CategoryRepositoryPort;
import com.techbloghub.persistance.entity.CategoryEntity;
import com.techbloghub.persistance.repository.CategoryRepository;
import com.techbloghub.persistance.repository.PostCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Repository
public class CategoryAdapter implements CategoryRepositoryPort {

    private final CategoryRepository categoryRepository;
    private final PostCategoryRepository postCategoryRepository;

    @Override
    public Category save(Category category) {
        CategoryEntity entity = category.getId() != null ? 
            CategoryEntity.fromDomainWithId(category) : 
            CategoryEntity.fromDomain(category);
        CategoryEntity savedEntity = categoryRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id)
                .map(CategoryEntity::toDomain);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name)
                .map(CategoryEntity::toDomain);
    }

    @Override
    public List<Category> findByNamesIn(Set<String> names) {
        return categoryRepository.findByNameIn(names).stream()
                .map(CategoryEntity::toDomain)
                .toList();
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryEntity::toDomain)
                .toList();
    }

    @Override
    public List<Category> findByNameContaining(String keyword) {
        return categoryRepository.findByNameContaining(keyword).stream()
                .map(CategoryEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }

    @Override
    public void deleteById(Long id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public List<Category> findUnusedCategories() {
        List<Long> unusedCategoryIds = postCategoryRepository.findUnusedCategoryIds();
        return categoryRepository.findAllById(unusedCategoryIds).stream()
                .map(CategoryEntity::toDomain)
                .toList();
    }

    @Override
    public List<Category> findTechCategories() {
        return categoryRepository.findTechCategories().stream()
                .map(CategoryEntity::toDomain)
                .toList();
    }
}