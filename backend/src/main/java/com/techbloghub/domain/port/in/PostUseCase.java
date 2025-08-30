package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 포스트 관련 비즈니스 유스케이스 인터페이스
 */
public interface PostUseCase {
    
    /**
     * 포스트 목록을 페이징하여 조회
     */
    Page<Post> getPosts(Pageable pageable);
    
    /**
     * 특정 블로그의 포스트 목록 조회
     */
    Page<Post> getPostsByBlog(Long blogId, Pageable pageable);
    
    /**
     * 최신 포스트 목록 조회
     */
    Page<Post> getRecentPosts(Pageable pageable);
}