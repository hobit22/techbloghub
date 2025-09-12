package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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
}