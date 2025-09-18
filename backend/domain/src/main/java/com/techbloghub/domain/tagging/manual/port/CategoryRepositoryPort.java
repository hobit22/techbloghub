package com.techbloghub.domain.tagging.manual.port;

import com.techbloghub.domain.tagging.manual.model.Category;

import java.util.List;
import java.util.Optional;

/**
 * Category Repository 포트 인터페이스
 * 헥사고날 아키텍처에서 도메인 레이어가 정의하는 아웃바운드 포트
 * 실제 구현은 Persistence 레이어에서 담당
 */
public interface CategoryRepositoryPort {

    /**
     * 모든 카테고리 조회
     */
    List<Category> findAll();
    
    /**
     * 이름으로 카테고리 조회
     */
    Optional<Category> findByName(String name);
    
    /**
     * 카테고리 저장
     */
    Category save(Category category);

}