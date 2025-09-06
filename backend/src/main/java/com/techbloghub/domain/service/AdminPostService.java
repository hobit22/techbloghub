package com.techbloghub.domain.service;

import com.techbloghub.admin.dto.AdminPostUpdateRequest;
import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.domain.port.in.AdminPostUseCase;
import com.techbloghub.domain.port.in.SearchUseCase;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 어드민 포스트 관리 서비스
 * AdminPostUseCase를 구현하여 어드민 포스트 관리 기능을 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminPostService implements AdminPostUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final PostDomainService postDomainService;
    private final SearchUseCase searchUseCase;

    @Override
    public Page<Post> searchPostsForAdmin(SearchCondition searchCondition, Pageable pageable) {
        log.debug("관리자용 포스트 검색: {}", searchCondition);
        return searchUseCase.searchPosts(searchCondition, pageable);
    }

    @Override
    public Optional<Post> getPostForAdmin(Long id) {
        log.debug("관리자용 포스트 조회: ID={}", id);
        return postRepositoryPort.findById(id);
    }

    /**
     * 포스트 수정
     */
    @Transactional
    public Post updatePost(Long id, AdminPostUpdateRequest request) {
        Post existingPost = postRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("포스트를 찾을 수 없습니다: " + id));

        // 도메인 서비스를 통한 업데이트
        Post updatedPost = postDomainService.updatePostWithDomainRules(
                existingPost, 
                request.getTitle(), 
                request.getContent(), 
                request.getAuthor()
        );

        Post savedPost = postRepositoryPort.save(updatedPost);
        
        log.info("포스트 업데이트 완료: ID={}, 제목={}", id, savedPost.getTitle());
        return savedPost;
    }

    /**
     * 포스트 삭제
     */
    @Transactional
    public void deletePost(Long id) {
        Post post = postRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("포스트를 찾을 수 없습니다: " + id));

        if (!postDomainService.canDeletePost(post)) {
            throw new IllegalArgumentException("포스트를 삭제할 수 없습니다: " + id);
        }

        postRepositoryPort.deleteById(id);
        log.info("포스트 삭제 완료: ID={}", id);
    }

    /**
     * 포스트 일괄 삭제
     */
    @Transactional
    public void deletePostsBatch(List<Long> ids) {
        for (Long id : ids) {
            Optional<Post> postOpt = postRepositoryPort.findById(id);
            if (postOpt.isPresent()) {
                Post post = postOpt.get();
                if (postDomainService.canDeletePost(post)) {
                    postRepositoryPort.deleteById(id);
                    log.info("일괄 삭제: 포스트 ID={}", id);
                } else {
                    log.warn("일괄 삭제 실패: 포스트 삭제 불가 ID={}", id);
                }
            } else {
                log.warn("일괄 삭제 실패: 포스트를 찾을 수 없음 ID={}", id);
            }
        }
        log.info("포스트 일괄 삭제 완료: 총 {}개 요청", ids.size());
    }

    /**
     * 포스트 제목/내용 검증
     */
    public boolean validatePost(Post post) {
        return postDomainService.validatePost(post);
    }

    /**
     * 포스트 존재 여부 확인
     */
    public boolean existsPost(Long id) {
        return postRepositoryPort.findById(id).isPresent();
    }
}