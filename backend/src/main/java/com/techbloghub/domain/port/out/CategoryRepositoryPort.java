package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 카테고리 저장소 아웃바운드 포트
 */
public interface CategoryRepositoryPort {
    
    /**
     * 카테고리 저장
     */
    Category save(Category category);
    
    /**
     * 카테고리 ID로 조회
     */
    Optional<Category> findById(Long id);
    
    /**
     * 카테고리명으로 조회
     */
    Optional<Category> findByName(String name);
    
    /**
     * 모든 카테고리 조회
     */
    List<Category> findAll();

}