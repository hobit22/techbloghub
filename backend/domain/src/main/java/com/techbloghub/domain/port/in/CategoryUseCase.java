package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Category;

import java.util.List;

/**
 * Category Use Case 인터페이스
 * 카테고리 관련 비즈니스 로직에 대한 입력 포트
 */
public interface CategoryUseCase {
    
    /**
     * 모든 카테고리 목록 조회
     * @return 전체 카테고리 목록
     */
    List<Category> getAllCategories();
}