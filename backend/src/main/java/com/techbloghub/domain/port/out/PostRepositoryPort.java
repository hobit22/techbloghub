package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 포스트 저장소 아웃바운드 포트
 */
public interface PostRepositoryPort {
    /**
     * 검색 조건으로 포스트 검색
     */
    Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable);
    
    /**
     * ID로 포스트 조회
     */
    Optional<Post> findById(Long id);
    
    /**
     * 포스트 저장
     */
    Post save(Post post);
    
    /**
     * 포스트 삭제
     */
    void deleteById(Long id);
    
    /**
     * 최신 포스트 조회 (발행일 기준 내림차순)
     */
    Page<Post> findAllByOrderByPublishedAtDesc(Pageable pageable);
    
    /**
     * 생성일 기준 내림차순으로 포스트 조회
     */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}