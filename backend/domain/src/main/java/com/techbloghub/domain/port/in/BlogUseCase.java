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
     * 블로그 ID로 단일 블로그 조회
     */
    Optional<Blog> getBlogById(Long id);

    /**
     * 블로그 통계 정보 조회
     */
    java.util.Map<String, Object> getBlogStats(Long blogId);

    /**
     * 새로운 블로그 생성
     */
    Blog createBlog(String name, String company, String rssUrl, String siteUrl, String logoUrl, String description);
}