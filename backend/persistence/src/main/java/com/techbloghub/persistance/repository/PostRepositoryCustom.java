package com.techbloghub.persistance.repository;

import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.persistance.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PostRepositoryCustom {

    Page<PostEntity> searchPosts(SearchCondition searchCondition, Pageable pageable);
    
    /**
     * ID로 포스트 조회 (Blog 정보 포함, 지연로딩 방지)
     */
    Optional<PostEntity> findByIdWithBlog(Long id);
    
    /**
     * 발행일 기준 내림차순으로 포스트 조회
     */
    Page<PostEntity> findAllByOrderByPublishedAtDesc(Pageable pageable);
    
    /**
     * 생성일 기준 내림차순으로 포스트 조회
     */
    Page<PostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}