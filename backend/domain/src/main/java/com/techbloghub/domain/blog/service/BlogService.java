package com.techbloghub.domain.blog.service;

import com.techbloghub.domain.blog.model.Blog;
import com.techbloghub.domain.blog.usecase.BlogUseCase;
import com.techbloghub.domain.blog.port.BlogRepositoryPort;
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

    @Override
    @Transactional
    public Blog createBlog(String name, String company, String rssUrl, String siteUrl, String logoUrl, String description) {
        log.debug("블로그 생성: name={}, company={}, rssUrl={}, logoUrl={}", name, company, rssUrl, logoUrl);

        // 새 블로그 도메인 모델 생성
        Blog newBlog = Blog.of(name, company, rssUrl, siteUrl, logoUrl, description);

        // 블로그 저장
        Blog savedBlog = blogRepositoryPort.save(newBlog);

        log.info("블로그가 성공적으로 생성되었습니다: ID={}, name={}", savedBlog.getId(), savedBlog.getName());
        return savedBlog;
    }
}