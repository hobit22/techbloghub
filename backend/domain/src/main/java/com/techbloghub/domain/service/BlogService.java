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
}