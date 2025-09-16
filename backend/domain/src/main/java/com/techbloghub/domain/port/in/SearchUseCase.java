package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

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

    /**
     * 포스트 ID로 단일 포스트 조회
     *
     * @param id 포스트 ID
     * @return 포스트 Optional
     */
    Optional<Post> getPostById(Long id);

    /**
     * 포스트 정보 수정
     *
     * @param post 수정할 포스트
     * @return 수정된 포스트
     */
    Optional<Post> updatePost(Post post);

    /**
     * 포스트 ID로 포스트 삭제
     *
     * @param id 삭제할 포스트 ID
     * @return 삭제 성공 여부
     */
    boolean deletePost(Long id);

    /**
     * 여러 포스트를 일괄 삭제
     *
     * @param ids 삭제할 포스트 ID 목록
     * @return 삭제된 포스트 개수
     */
    int deletePostsBatch(java.util.List<Long> ids);
}