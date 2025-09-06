package com.techbloghub.persistance.adapter;

import com.techbloghub.domain.model.Blog;
import com.techbloghub.domain.model.BlogStatus;
import com.techbloghub.domain.port.out.BlogRepositoryPort;
import com.techbloghub.persistance.entity.BlogEntity;
import com.techbloghub.persistance.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class BlogAdapter implements BlogRepositoryPort {
    private final BlogRepository blogRepository;

    @Override
    public Blog save(Blog blog) {
        BlogEntity entity = BlogEntity.from(blog);
        BlogEntity savedEntity = blogRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<Blog> findById(Long id) {
        return blogRepository.findById(id).map(BlogEntity::toDomain);
    }

    @Override
    public Page<Blog> findAll(Pageable pageable) {
        return blogRepository.findAll(pageable).map(BlogEntity::toDomain);
    }

    @Override
    public List<Blog> findAll() {
        return blogRepository.findAll()
                .stream()
                .map(BlogEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Blog> findActiveBlogs() {
        return blogRepository.findActiveBlogs()
                .stream()
                .map(BlogEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Blog> findByCompany(String company) {
        return blogRepository.findByCompanyContainingIgnoreCase(company)
                .stream()
                .map(BlogEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Blog> findBlogsNeedingCrawl() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return blogRepository.findByStatus(BlogStatus.ACTIVE)
                .stream()
                .filter(entity -> entity.getLastCrawledAt() == null ||
                        entity.getLastCrawledAt().isBefore(oneHourAgo))
                .map(BlogEntity::toDomain)
                .collect(Collectors.toList());
    }

}
