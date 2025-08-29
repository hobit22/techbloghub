package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Category;

import java.util.List;
import java.util.Optional;

/**
 * 카테고리 관련 비즈니스 유스케이스 인터페이스
 */
public interface CategoryUseCase {
    
    /**
     * 모든 카테고리 조회
     */
    List<Category> getAllCategories();
    
    /**
     * 카테고리 ID로 조회
     */
    Optional<Category> getCategoryById(Long id);
    
    /**
     * 카테고리명으로 조회
     */
    Optional<Category> getCategoryByName(String name);
    
    /**
     * 카테고리 저장
     */
    Category saveCategory(Category category);
}