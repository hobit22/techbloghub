package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.domain.port.in.SearchUseCase;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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