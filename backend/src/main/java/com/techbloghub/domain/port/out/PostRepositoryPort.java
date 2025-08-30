package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 포스트 저장소 아웃바운드 포트
 */
public interface PostRepositoryPort {
    
    /**
     * 포스트 ID로 조회
     */
    Optional<Post> findById(Long id);
    
    /**
     * 모든 포스트를 페이징하여 조회 (발행일 기준 내림차순)
     */
    Page<Post> findAllOrderByPublishedAtDesc(Pageable pageable);
    
    /**
     * 블로그별 포스트 조회
     */
    Page<Post> findByBlogIdOrderByPublishedAtDesc(Long blogId, Pageable pageable);
    
    /**
     * 최신 포스트 조회
     */
    List<Post> findRecentPosts(LocalDateTime since);
    
    /**
     * 태그로 포스트 검색
     */
    Page<Post> findByTagNames(List<String> tagNames, Pageable pageable);
    
    /**
     * 카테고리로 포스트 검색
     */
    Page<Post> findByCategoryNames(List<String> categoryNames, Pageable pageable);
    
    /**
     * 키워드로 포스트 검색
     */
    Page<Post> searchByKeyword(String keyword, Pageable pageable);
    
    /**
     * 복합 조건으로 포스트 검색
     */
    Page<Post> searchWithFilters(String keyword, List<String> companies, 
                                List<String> tags, List<String> categories, 
                                Pageable pageable);
    /**
     * 블로그별 포스트 수 조회
     */
    long countByBlogId(Long blogId);
}