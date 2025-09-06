package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Blog;
import com.techbloghub.domain.port.in.AdminBlogUseCase;
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

/**
 * 어드민 블로그 관리 서비스
 * AdminBlogUseCase를 구현하여 어드민 블로그 관리 기능을 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminBlogService implements AdminBlogUseCase {

    private final BlogRepositoryPort blogRepositoryPort;
    private final BlogDomainService blogDomainService;
    private final BlogUseCase blogUseCase;

    @Override
    public Page<Blog> getAllBlogsForAdmin(Pageable pageable) {
        log.debug("관리자용 블로그 목록 조회");
        return blogUseCase.getAllBlogs(pageable);
    }

    @Override
    public List<Blog> getActiveBlogsForAdmin() {
        log.debug("관리자용 활성 블로그 목록 조회");
        return blogUseCase.getActiveBlogs();
    }

    @Override
    public Optional<Blog> getBlogForAdmin(Long id) {
        log.debug("관리자용 블로그 조회: ID={}", id);
        return blogUseCase.getBlogById(id);
    }

    @Override
    @Transactional
    public void triggerBlogRecrawling(Long blogId) {
        Blog blog = blogRepositoryPort.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + blogId));

        if (!blogDomainService.canCrawlBlog(blog)) {
            throw new IllegalArgumentException("크롤링할 수 없는 블로그입니다: " + blogId);
        }

        // TODO: Lambda 크롤러 호출 로직 구현
        log.info("블로그 재크롤링 트리거: ID={}, 블로그={}", blogId, blog.getName());
    }

    @Override
    @Transactional
    public void triggerAllBlogsRecrawling() {
        List<Blog> activeBlogs = getActiveBlogsForAdmin();
        
        long crawlableCount = activeBlogs.stream()
                .mapToLong(blog -> blogDomainService.canCrawlBlog(blog) ? 1 : 0)
                .sum();

        // TODO: Lambda 크롤러 일괄 호출 로직 구현
        log.info("전체 블로그 재크롤링 트리거: 활성 블로그 {}개 중 크롤링 가능 {}개", 
                activeBlogs.size(), crawlableCount);
    }

    @Override
    @Transactional
    public Blog updateBlogStatus(Long id, String status) {
        Blog existingBlog = blogRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + id));

        if (!blogDomainService.canChangeStatus(existingBlog, status)) {
            throw new IllegalArgumentException("블로그 상태를 변경할 수 없습니다: " + id);
        }

        // TODO: Blog 엔티티에 상태 변경 메서드 추가 필요
        log.info("블로그 상태 업데이트: ID={}, 기존상태={}, 새상태={}", 
                id, existingBlog.getStatus(), status);
        
        // 현재는 기존 블로그 반환 (실제 업데이트 로직 구현 필요)
        return existingBlog;
    }

    @Override
    public Object getBlogStats(Long id) {
        Blog blog = blogRepositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + id));

        // TODO: 블로그 통계 구현 (포스트 수, 최근 크롤링 상태, 크롤링 간격 등)
        log.debug("블로그 통계 조회: ID={}, 블로그={}", id, blog.getName());
        
        return new BlogStats(
                id,
                blog.getName(),
                0, // 포스트 수 (구현 필요)
                blogDomainService.isActiveBlog(blog),
                blogDomainService.needsCrawling(blog),
                blogDomainService.calculateCrawlingInterval(blog),
                blogDomainService.calculateBlogPriority(blog)
        );
    }

    @Override
    public Object getAllBlogsStats() {
        List<Blog> allBlogs = getActiveBlogsForAdmin();
        
        long totalBlogs = allBlogs.size();
        long activeBlogs = allBlogs.stream()
                .mapToLong(blog -> blogDomainService.isActiveBlog(blog) ? 1 : 0)
                .sum();
        long crawlableBlogs = allBlogs.stream()
                .mapToLong(blog -> blogDomainService.canCrawlBlog(blog) ? 1 : 0)
                .sum();

        log.debug("전체 블로그 통계 조회: 전체={}, 활성={}, 크롤링가능={}", 
                totalBlogs, activeBlogs, crawlableBlogs);

        return new AllBlogsStats(totalBlogs, activeBlogs, crawlableBlogs);
    }

    /**
     * 블로그 통계 응답 DTO
     */
    public record BlogStats(
            Long blogId,
            String blogName,
            int postCount,
            boolean isActive,
            boolean needsCrawling,
            long crawlingInterval,
            int priority
    ) {}

    /**
     * 전체 블로그 통계 응답 DTO
     */
    public record AllBlogsStats(
            long totalBlogs,
            long activeBlogs,
            long crawlableBlogs
    ) {}
}