package com.techbloghub.persistence.adapter;

import com.techbloghub.domain.blog.model.Blog;
import com.techbloghub.domain.blog.port.BlogRepositoryPort;
import com.techbloghub.persistence.entity.BlogEntity;
import com.techbloghub.persistence.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
@Slf4j
public class BlogAdapter implements BlogRepositoryPort {
    private final BlogRepository blogRepository;

    @Override
    public Optional<Blog> findById(Long id) {
        return blogRepository.findById(id).map(BlogEntity::toDomain);
    }

    @Override
    public Page<Blog> findAll(Pageable pageable) {
        return blogRepository.findAll(pageable).map(BlogEntity::toDomain);
    }

    @Override
    @Cacheable("allBlogs")
    public List<Blog> findAll() {
        return blogRepository.findAll()
                .stream()
                .map(BlogEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateLastCrawledAt(Long blogId, LocalDateTime lastCrawledAt) {
        BlogEntity blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("Blog not found: " + blogId));

        blog.updateLastCrawledAt(lastCrawledAt);
        blogRepository.save(blog);

        log.debug("Updated last crawled time for blog {} to {}", blogId, lastCrawledAt);
    }

    @Override
    @Transactional
    public void incrementFailureCount(Long blogId) {
        BlogEntity blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("Blog not found: " + blogId));

        blog.incrementFailureCount();
        blogRepository.save(blog);

        log.warn("Incremented failure count for blog {} to {}", blogId, blog.getFailureCount());
    }

    @Override
    @Transactional
    public void resetFailureCount(Long blogId) {
        BlogEntity blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("Blog not found: " + blogId));

        blog.resetFailureCount();
        blogRepository.save(blog);

        log.debug("Reset failure count for blog {}", blogId);
    }

    @Override
    @Transactional
    public Blog save(Blog blog) {
        BlogEntity blogEntity = BlogEntity.from(blog);
        BlogEntity savedEntity = blogRepository.save(blogEntity);

        log.debug("Saved blog: ID={}, name={}", savedEntity.getId(), savedEntity.getName());
        return savedEntity.toDomain();
    }
}
