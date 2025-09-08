package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 어드민 포스트 관리 UseCase
 * 관리자를 위한 포스트 CRUD 및 관리 기능을 정의
 */
public interface AdminPostUseCase {
    
    /**
     * 관리자용 포스트 검색
     */
    Page<Post> searchPostsForAdmin(SearchCondition searchCondition, Pageable pageable);
    
    /**
     * 관리자용 포스트 조회
     */
    Optional<Post> getPostForAdmin(Long id);
    
    /**
     * 포스트 수정
     */
    Post updatePost(Long id, String title, String content, String author, java.util.Set<String> tags, java.util.Set<String> categories, String status);
    
    /**
     * 포스트 삭제
     */
    void deletePost(Long id);
    
    /**
     * 포스트 일괄 삭제
     */
    void deletePostsBatch(List<Long> ids);
    
    /**
     * 포스트 존재 여부 확인
     */
    boolean existsPost(Long id);
    
    /**
     * 포스트 검증
     */
    boolean validatePost(Post post);
}