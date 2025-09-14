package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.domain.model.TaggingProcessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 포스트 저장소 아웃바운드 포트
 */
public interface PostRepositoryPort {
    /**
     * 검색 조건으로 포스트 검색
     */
    Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable);


    /**
     * 포스트를 저장
     *
     * @param post 저장할 포스트
     * @return 저장된 포스트 ID
     */
    Long savePost(Post post);

    /**
     * 정규화된 URL의 중복 여부 확인
     *
     * @param normalizedUrl 정규화된 URL
     * @return 중복 여부
     */
    boolean existsByNormalizedUrl(String normalizedUrl);

    /**
     * 포스트의 태깅 처리 상태 업데이트
     *
     * @param postId 포스트 ID
     * @param status 업데이트할 태깅 상태
     */
    void updateTaggingStatus(Long postId, TaggingProcessStatus status);

    /**
     * 포스트 ID로 포스트 조회
     *
     * @param postId 포스트 ID
     * @return 포스트 (없으면 Optional.empty())
     */
    Optional<Post> findById(Long postId);
}