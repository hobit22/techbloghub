package com.techbloghub.domain.post.port;

import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.post.model.SearchCondition;
import com.techbloghub.domain.tagging.auto.model.TaggingProcessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
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

    List<Post> findByTaggingStatus(TaggingProcessStatus taggingProcessStatus, int limit);

    /**
     * 태깅 처리 상태별 포스트 개수 통계
     *
     * @return 상태별 포스트 개수 Map
     */
    Map<TaggingProcessStatus, Long> getTaggingStatusStatistics();

    /**
     * 포스트 ID로 포스트 삭제
     *
     * @param postId 삭제할 포스트 ID
     * @return 삭제 성공 여부
     */
    boolean deleteById(Long postId);

    /**
     * 여러 포스트를 일괄 삭제
     *
     * @param postIds 삭제할 포스트 ID 목록
     * @return 삭제된 포스트 개수
     */
    int deleteByIds(List<Long> postIds);

    /**
     * 포스트 정보 업데이트
     *
     * @param post 업데이트할 포스트
     * @return 업데이트된 포스트
     */
    Optional<Post> updatePost(Post post);

    /**
     * total_content가 있는 포스트들을 조회 (테스트용)
     *
     * @param limit 최대 조회 개수
     * @return total_content가 있는 포스트 목록
     */
    List<Post> findPostsWithTotalContent(int limit);
}