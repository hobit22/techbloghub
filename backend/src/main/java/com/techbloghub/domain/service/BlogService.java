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

/**
 * 블로그 관리 관련 비즈니스 로직을 처리하는 애플리케이션 서비스
 * BlogUseCase 인터페이스를 구현하여 블로그 도메인 로직을 수행
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BlogService implements BlogUseCase {

    private final BlogRepositoryPort blogRepositoryPort;

    @Override
    public Page<Blog> getAllBlogs(Pageable pageable) {
        log.debug("Fetching all blogs with pageable: {}", pageable);
        return blogRepositoryPort.findAll(pageable);
    }

    @Override
    public List<Blog> getActiveBlogs() {
        log.debug("Fetching active blogs");
        return blogRepositoryPort.findActiveBlogs();
    }
}