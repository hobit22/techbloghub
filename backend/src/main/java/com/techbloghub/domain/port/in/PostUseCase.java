package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 일반 포스트 관리 UseCase
 * 일반 사용자를 위한 포스트 조회 및 검색 기능을 정의
 */
public interface PostUseCase {
    
    /**
     * 검색 조건으로 포스트 검색
     */
    Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable);
    
    /**
     * ID로 포스트 조회
     */
    Optional<Post> getPostById(Long id);
    
    /**
     * 최신 포스트 조회
     */
    Page<Post> getRecentPosts(Pageable pageable);
    
    /**
     * 인기 포스트 조회
     */
    Page<Post> getPopularPosts(Pageable pageable);
}