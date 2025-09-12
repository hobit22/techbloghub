package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Tag;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Tag Repository 포트 인터페이스
 * 헥사고날 아키텍처에서 도메인 레이어가 정의하는 아웃바운드 포트
 * 실제 구현은 Persistence 레이어에서 담당
 */
public interface TagRepositoryPort {
    
    /**
     * 모든 태그 조회
     */
    List<Tag> findAll();
    
    /**
     * 이름 패턴으로 태그 검색
     */
    List<Tag> findByNameContaining(String keyword);
}