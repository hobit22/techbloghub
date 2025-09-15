package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Blog;
import com.techbloghub.domain.port.in.BlogUseCase;
import com.techbloghub.domain.port.out.BlogRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 블로그 서비스
 * BlogUseCase를 구현하여 일반 사용자를 위한 블로그 관리 기능을 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BlogService implements BlogUseCase {

    private final BlogRepositoryPort blogRepositoryPort;

    @Override
    public Page<Blog> getAllBlogs(Pageable pageable) {
        log.debug("블로그 목록 조회");
        return blogRepositoryPort.findAll(pageable);
    }

    @Override
    public List<Blog> getActiveBlogs() {
        log.debug("활성 블로그 목록 조회");
        List<Blog> allBlogs = blogRepositoryPort.findAll();

        return allBlogs.stream()
                .filter(Blog::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Blog> getBlogById(Long id) {
        log.debug("블로그 단일 조회: ID={}", id);
        return blogRepositoryPort.findById(id);
    }

    @Override
    public java.util.Map<String, Object> getBlogStats(Long blogId) {
        log.debug("블로그 통계 조회: ID={}", blogId);
        if (blogId == null) {
            throw new IllegalArgumentException("블로그 ID는 필수입니다.");
        }

        // 블로그 존재 여부 확인
        Optional<Blog> blog = blogRepositoryPort.findById(blogId);
        if (blog.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 블로그입니다: ID=" + blogId);
        }

        // 기본 통계 정보 반환 (추후 실제 통계 로직 구현)
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("blogId", blogId);
        stats.put("blogName", blog.get().getName());
        stats.put("totalPosts", 0); // TODO: 실제 포스트 개수 조회
        stats.put("lastCrawledAt", blog.get().getLastCrawledAt());
        stats.put("status", blog.get().getStatus());

        return stats;
    }
}