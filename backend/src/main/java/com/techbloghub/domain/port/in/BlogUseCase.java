package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 블로그 관리 관련 비즈니스 유스케이스 인터페이스
 */
public interface BlogUseCase {
    
    /**
     * 모든 블로그 목록을 페이징하여 조회
     */
    Page<Blog> getAllBlogs(Pageable pageable);
    
    /**
     * 활성화된 블로그 목록 조회
     */
    List<Blog> getActiveBlogs();
    
    /**
     * 블로그 ID로 단건 조회
     */
    Optional<Blog> getBlogById(Long id);
    
    /**
     * 회사별 블로그 목록 조회
     */
    List<Blog> getBlogsByCompany(String company);
    
    /**
     * 블로그 저장/수정
     */
    Blog saveBlog(Blog blog);
    
    /**
     * 블로그 상태 변경
     */
    Blog updateBlogStatus(Long blogId, String status);
}