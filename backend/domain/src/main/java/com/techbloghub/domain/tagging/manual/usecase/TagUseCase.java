package com.techbloghub.domain.tagging.manual.usecase;

import com.techbloghub.domain.tagging.manual.model.Tag;

import java.util.List;

/**
 * Tag Use Case 인터페이스
 * 태그 관련 비즈니스 로직에 대한 입력 포트
 */
public interface TagUseCase {
    
    /**
     * 모든 태그 목록 조회
     * @return 전체 태그 목록
     */
    List<Tag> getAllTags();
    
    /**
     * 태그 검색
     * @param query 검색어 (null이면 전체 태그 반환)
     * @return 검색 조건에 맞는 태그 목록
     */
    List<Tag> searchTags(String query);
}