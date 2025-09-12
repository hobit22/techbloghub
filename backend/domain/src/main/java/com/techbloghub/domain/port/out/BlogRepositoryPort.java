package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 블로그 저장소 아웃바운드 포트
 */
public interface BlogRepositoryPort {

    /**
     * 블로그 ID로 조회
     */
    Optional<Blog> findById(Long id);

    /**
     * 모든 블로그를 페이징하여 조회
     */
    Page<Blog> findAll(Pageable pageable);

    /**
     * 모든 블로그 목록 조회
     */
    List<Blog> findAll();

    void resetFailureCount(Long blogId);

    void incrementFailureCount(Long blogId);

    void updateLastCrawledAt(Long blogId, LocalDateTime crawlStartTime);
}