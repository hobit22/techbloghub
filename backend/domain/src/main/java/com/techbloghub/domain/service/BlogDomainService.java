package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Blog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 블로그 도메인 서비스
 * 순수한 비즈니스 로직만을 담당하는 도메인 서비스
 */
@Service
@Slf4j
public class BlogDomainService {

    /**
     * 블로그 활성 상태 검증
     */
    public boolean isActiveBlog(Blog blog) {
        if (blog == null) {
            return false;
        }
        return blog.isActive();
    }

    /**
     * 크롤링 필요 여부 판단 (도메인 규칙)
     */
    public boolean needsCrawling(Blog blog) {
        if (blog == null) {
            return false;
        }
        return blog.needsCrawling();
    }

    /**
     * RSS URL 유효성 검증
     */
    public boolean hasValidRssUrl(Blog blog) {
        if (blog == null) {
            return false;
        }
        return blog.hasValidRssUrl();
    }

    /**
     * 블로그 크롤링 가능 여부 검증
     */
    public boolean canCrawlBlog(Blog blog) {
        if (!isActiveBlog(blog)) {
            log.warn("비활성 블로그 크롤링 시도: {}", blog != null ? blog.getName() : "null");
            return false;
        }

        if (!hasValidRssUrl(blog)) {
            log.warn("잘못된 RSS URL 블로그 크롤링 시도: {}", blog.getName());
            return false;
        }

        return true;
    }

    /**
     * 블로그 상태 변경 검증
     */
    public boolean canChangeStatus(Blog blog, String newStatus) {
        if (blog == null) {
            return false;
        }

        // 비즈니스 규칙: 예를 들어 특정 조건에서만 상태 변경 허용
        // 현재는 모든 상태 변경 허용
        log.debug("블로그 상태 변경 요청: {} -> {}", 
                  blog.getStatus(), newStatus);
        
        return true;
    }

    /**
     * 크롤링 간격 계산 (도메인 규칙)
     */
    public long calculateCrawlingInterval(Blog blog) {
        if (blog == null) {
            return 3600; // 기본 1시간
        }

        // 블로그 특성에 따른 크롤링 간격 조정 로직
        // 현재는 기본값 사용
        return 3600; // 1시간 (초 단위)
    }

    /**
     * 블로그 우선순위 계산
     */
    public int calculateBlogPriority(Blog blog) {
        if (blog == null || !isActiveBlog(blog)) {
            return 0;
        }

        // 블로그 중요도에 따른 우선순위 계산
        // 현재는 모든 블로그 동일 우선순위
        return 1;
    }
}