package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.Tag;
import com.techbloghub.domain.port.in.PostUseCase;
import com.techbloghub.domain.port.out.CategoryRepositoryPort;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import com.techbloghub.domain.port.out.TagRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 포스트 관련 비즈니스 로직을 처리하는 애플리케이션 서비스
 * PostUseCase 인터페이스를 구현하여 도메인 비즈니스 로직을 수행
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostService implements PostUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final TagRepositoryPort tagRepositoryPort;

    @Override
    public Page<Post> getPosts(Pageable pageable) {
        log.debug("Fetching posts with pageable: {}", pageable);
        return postRepositoryPort.findAllOrderByPublishedAtDesc(pageable);
    }

    @Override
    public Page<Post> getPostsByBlog(Long blogId, Pageable pageable) {
        log.debug("Fetching posts by blog id: {} with pageable: {}", blogId, pageable);
        return postRepositoryPort.findByBlogIdOrderByPublishedAtDesc(blogId, pageable);
    }

    @Override
    public Page<Post> getRecentPosts(Pageable pageable) {
        log.debug("Fetching recent posts with pageable: {}", pageable);
        // 7일 이내 포스트를 최신순으로 조회
        return postRepositoryPort.findAllOrderByPublishedAtDesc(pageable);
    }
}