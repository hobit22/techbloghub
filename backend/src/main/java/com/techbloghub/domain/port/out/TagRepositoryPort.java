package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Tag;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 태그 저장소 아웃바운드 포트
 */
public interface TagRepositoryPort {
    
    /**
     * 태그 저장
     */
    Tag save(Tag tag);
    
    /**
     * 태그 ID로 조회
     */
    Optional<Tag> findById(Long id);
    
    /**
     * 태그명으로 조회
     */
    Optional<Tag> findByName(String name);
    
    /**
     * 모든 태그 조회
     */
    List<Tag> findAll();
    
    /**
     * 텍스트에서 태그를 추출하고 생성
     */
    Set<Tag> extractAndCreateTags(String text);
}