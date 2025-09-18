package com.techbloghub.domain.tagging.auto.port;

import java.util.List;

/**
 * Rejected Category Repository 포트 인터페이스
 * LLM이 제안했으나 거부된 카테고리들을 관리
 */
public interface RejectedCategoryRepositoryPort {
    
    /**
     * 거부된 카테고리 저장
     */
    void saveRejectedCategory(Long postId, String categoryName, String reason);
    
    /**
     * 모든 거부된 카테고리 조회 (분석용)
     */
    List<String> findAllRejectedCategories();
}