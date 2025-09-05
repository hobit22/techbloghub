package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 포스트 도메인 서비스
 * 순수한 비즈니스 로직만을 담당하는 도메인 서비스
 */
@Service
@Slf4j
public class PostDomainService {

    /**
     * 포스트 업데이트를 위한 도메인 로직
     */
    public Post updatePostWithDomainRules(Post existingPost, String newTitle, String newContent, String newAuthor) {
        // 도메인 규칙 검증
        validatePostUpdate(existingPost, newTitle, newContent);
        
        return Post.builder()
                .id(existingPost.getId())
                .title(newTitle != null ? newTitle : existingPost.getTitle())
                .content(newContent != null ? newContent : existingPost.getContent())
                .originalUrl(existingPost.getOriginalUrl()) // URL은 변경하지 않음
                .author(newAuthor != null ? newAuthor : existingPost.getAuthor())
                .publishedAt(existingPost.getPublishedAt()) // 발행일은 변경하지 않음
                .createdAt(existingPost.getCreatedAt()) // 생성일은 변경하지 않음
                .updatedAt(LocalDateTime.now()) // 수정일은 현재 시간으로 업데이트
                .blog(existingPost.getBlog()) // 블로그는 변경하지 않음
                .build();
    }

    /**
     * 포스트 제목/내용 검증
     */
    public boolean validatePost(Post post) {
        if (post == null) {
            log.warn("포스트 검증 실패: 포스트가 null");
            return false;
        }
        
        if (post.getTitle() == null || post.getTitle().trim().isEmpty()) {
            log.warn("포스트 검증 실패: 제목이 비어있음 ID={}", post.getId());
            return false;
        }
        
        if (post.getOriginalUrl() == null || post.getOriginalUrl().trim().isEmpty()) {
            log.warn("포스트 검증 실패: URL이 비어있음 ID={}", post.getId());
            return false;
        }
        
        return true;
    }

    /**
     * 포스트가 최신인지 확인 (도메인 규칙)
     */
    public boolean isRecentPost(Post post) {
        if (post.getPublishedAt() == null) return false;
        return post.getPublishedAt().isAfter(LocalDateTime.now().minusDays(7));
    }

    /**
     * 포스트 업데이트 검증
     */
    private void validatePostUpdate(Post existingPost, String newTitle, String newContent) {
        if (existingPost == null) {
            throw new IllegalArgumentException("기존 포스트가 존재하지 않습니다");
        }

        // 제목이 제공되었는데 비어있는 경우
        if (newTitle != null && newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("포스트 제목은 비어있을 수 없습니다");
        }

        // 비즈니스 규칙: 제목 길이 제한 (예: 200자)
        if (newTitle != null && newTitle.length() > 200) {
            throw new IllegalArgumentException("포스트 제목은 200자를 초과할 수 없습니다");
        }

        // 비즈니스 규칙: 콘텐츠 길이 제한 (예: 50000자)
        if (newContent != null && newContent.length() > 50000) {
            throw new IllegalArgumentException("포스트 내용은 50,000자를 초과할 수 없습니다");
        }

        log.debug("포스트 업데이트 검증 통과: ID={}", existingPost.getId());
    }

    /**
     * 포스트 삭제 가능 여부 검증
     */
    public boolean canDeletePost(Post post) {
        if (post == null) {
            return false;
        }

        // 비즈니스 규칙: 예를 들어, 너무 최근 포스트는 삭제 불가 등
        // 현재는 모든 포스트 삭제 허용
        return true;
    }
}