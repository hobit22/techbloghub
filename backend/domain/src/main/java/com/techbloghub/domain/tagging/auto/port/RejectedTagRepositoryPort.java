package com.techbloghub.domain.tagging.auto.port;

import java.util.List;

/**
 * Rejected Tag Repository 포트 인터페이스
 * LLM이 제안했으나 거부된 태그들을 관리
 */
public interface RejectedTagRepositoryPort {
    
    /**
     * 거부된 태그 저장
     */
    void saveRejectedTag(Long postId, String tagName, String reason);
    
    /**
     * 모든 거부된 태그 조회 (분석용)
     */
    List<String> findAllRejectedTags();
}