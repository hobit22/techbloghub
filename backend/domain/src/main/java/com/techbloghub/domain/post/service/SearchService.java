package com.techbloghub.domain.post.service;

import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.post.model.SearchCondition;
import com.techbloghub.domain.post.usecase.SearchUseCase;
import com.techbloghub.domain.post.port.PostRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 포스트 검색 관련 비즈니스 로직을 처리하는 도메인 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchService implements SearchUseCase {

    private final PostRepositoryPort postRepositoryPort;

    @Override
    public Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable) {
        log.debug("Searching posts with condition: {} and pageable: {}", searchCondition, pageable);

        validateSearchCondition(searchCondition);

        return postRepositoryPort.searchPosts(searchCondition, pageable);
    }

    @Override
    public Optional<Post> getPostById(Long id) {
        log.debug("포스트 단일 조회: ID={}", id);
        if (id == null) {
            throw new IllegalArgumentException("포스트 ID는 필수입니다.");
        }
        return postRepositoryPort.findById(id);
    }

    @Override
    @Transactional
    public Optional<Post> updatePost(Post post) {
        log.debug("포스트 수정: ID={}", post.getId());
        if (post == null || post.getId() == null) {
            throw new IllegalArgumentException("포스트와 포스트 ID는 필수입니다.");
        }

        // 기존 포스트 존재 여부 확인
        Optional<Post> existingPost = postRepositoryPort.findById(post.getId());
        if (existingPost.isEmpty()) {
            log.warn("수정하려는 포스트를 찾을 수 없습니다: ID={}", post.getId());
            return Optional.empty();
        }

        return postRepositoryPort.updatePost(post);
    }

    @Override
    @Transactional
    public boolean deletePost(Long id) {
        log.debug("포스트 삭제: ID={}", id);
        if (id == null) {
            throw new IllegalArgumentException("포스트 ID는 필수입니다.");
        }

        // 포스트 존재 여부 확인
        Optional<Post> existingPost = postRepositoryPort.findById(id);
        if (existingPost.isEmpty()) {
            log.warn("삭제하려는 포스트를 찾을 수 없습니다: ID={}", id);
            return false;
        }

        return postRepositoryPort.deleteById(id);
    }

    @Override
    @Transactional
    public int deletePostsBatch(java.util.List<Long> ids) {
        log.debug("포스트 일괄 삭제: 개수={}", ids != null ? ids.size() : 0);
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("삭제할 포스트 ID 목록이 필요합니다.");
        }

        // 유효하지 않은 ID 제거
        java.util.List<Long> validIds = ids.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (validIds.isEmpty()) {
            throw new IllegalArgumentException("유효한 포스트 ID가 없습니다.");
        }

        return postRepositoryPort.deleteByIds(validIds);
    }

    private void validateSearchCondition(SearchCondition searchCondition) {
        if (searchCondition == null) {
            throw new IllegalArgumentException("검색 조건은 필수입니다.");
        }

        if (!hasAnySearchCriteria(searchCondition)) {
            log.info("검색 조건이 없어 모든 포스트를 조회합니다.");
        }
    }

    private boolean hasAnySearchCriteria(SearchCondition searchCondition) {
        return searchCondition.hasKeyword()
                || searchCondition.hasTags()
                || searchCondition.hasCategories()
                || searchCondition.hasBlogIds()
                || searchCondition.hasDateRange();
    }
}