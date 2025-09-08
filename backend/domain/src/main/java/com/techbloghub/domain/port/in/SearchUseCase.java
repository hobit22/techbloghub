package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 포스트 검색 관련 유스케이스 인터페이스
 */
public interface SearchUseCase {

    /**
     * 검색 조건에 따라 포스트를 검색합니다.
     *
     * @param searchCondition 검색 조건
     * @param pageable        페이징 정보
     * @return 검색된 포스트 페이지
     */
    Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable);
}