package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.port.in.CategoryUseCase;
import com.techbloghub.domain.port.out.CategoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 카테고리 관리 관련 비즈니스 로직을 처리하는 애플리케이션 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService implements CategoryUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;

    @Override
    public List<Category> getAllCategories() {
        log.debug("Fetching all categories");
        return categoryRepositoryPort.findAll();
    }

    @Override
    public Optional<Category> getCategoryById(Long id) {
        log.debug("Fetching category by id: {}", id);
        return categoryRepositoryPort.findById(id);
    }

    @Override
    public Optional<Category> getCategoryByName(String name) {
        log.debug("Fetching category by name: {}", name);
        return categoryRepositoryPort.findByName(name);
    }

    @Override
    @Transactional
    public Category saveCategory(Category category) {
        log.debug("Saving category: {}", category.getName());
        
        if (!category.isValid()) {
            throw new IllegalArgumentException("Invalid category data");
        }
        
        return categoryRepositoryPort.save(category);
    }
}