package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 포스트 저장소 아웃바운드 포트
 */
public interface PostRepositoryPort {
    /**
     * 검색 조건으로 포스트 검색
     */
    Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable);
}