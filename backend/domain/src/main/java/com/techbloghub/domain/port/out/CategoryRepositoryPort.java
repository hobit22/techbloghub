package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Category;

import java.util.List;

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
}