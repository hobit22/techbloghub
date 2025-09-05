package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.domain.port.in.PostUseCase;
import com.techbloghub.domain.port.in.SearchUseCase;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 포스트 서비스
 * PostUseCase를 구현하여 일반 사용자를 위한 포스트 관리 기능을 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostService implements PostUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final PostDomainService postDomainService;
    private final SearchUseCase searchUseCase;

    @Override
    public Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable) {
        log.debug("포스트 검색: {}", searchCondition);
        return searchUseCase.searchPosts(searchCondition, pageable);
    }

    @Override
    public Optional<Post> getPostById(Long id) {
        log.debug("포스트 조회: ID={}", id);
        return postRepositoryPort.findById(id);
    }

    @Override
    public Page<Post> getRecentPosts(Pageable pageable) {
        log.debug("최신 포스트 조회");
        // TODO: 최신 포스트 조회 로직 구현 (publishedAt 기준 내림차순)
        return postRepositoryPort.findAllByOrderByPublishedAtDesc(pageable);
    }

    @Override
    public Page<Post> getPopularPosts(Pageable pageable) {
        log.debug("인기 포스트 조회");
        // TODO: 인기 포스트 조회 로직 구현 (조회수, 좋아요 등 기준)
        // 현재는 최신순으로 반환
        return postRepositoryPort.findAllByOrderByCreatedAtDesc(pageable);
    }
}