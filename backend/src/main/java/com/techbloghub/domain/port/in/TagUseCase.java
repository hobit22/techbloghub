package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Tag;

import java.util.List;
import java.util.Optional;

/**
 * 태그 관련 비즈니스 유스케이스 인터페이스
 */
public interface TagUseCase {
    
    /**
     * 모든 태그 조회
     */
    List<Tag> getAllTags();
    
    /**
     * 태그 ID로 조회
     */
    Optional<Tag> getTagById(Long id);
    
    /**
     * 태그명으로 조회
     */
    Optional<Tag> getTagByName(String name);
    
    /**
     * 태그 저장
     */
    Tag saveTag(Tag tag);
}