package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 어드민 블로그 관리 UseCase
 * 관리자를 위한 블로그 관리 및 크롤링 기능을 정의
 */
public interface AdminBlogUseCase {
    
    /**
     * 관리자용 블로그 목록 조회 (페이징)
     */
    Page<Blog> getAllBlogsForAdmin(Pageable pageable);
    
    /**
     * 관리자용 활성 블로그 목록 조회
     */
    List<Blog> getActiveBlogsForAdmin();
    
    /**
     * 관리자용 블로그 상세 조회
     */
    Optional<Blog> getBlogForAdmin(Long id);
    
    /**
     * 블로그 재크롤링 트리거
     */
    void triggerBlogRecrawling(Long blogId);
    
    /**
     * 전체 블로그 재크롤링 트리거
     */
    void triggerAllBlogsRecrawling();
    
    /**
     * 블로그 상태 업데이트
     */
    Blog updateBlogStatus(Long id, String status);
    
    /**
     * 블로그 통계 조회
     */
    Object getBlogStats(Long id);
    
    /**
     * 전체 블로그 통계 조회
     */
    Object getAllBlogsStats();
}