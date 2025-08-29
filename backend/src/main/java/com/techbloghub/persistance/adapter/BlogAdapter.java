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
        BlogEntity entity = toEntity(blog);
        BlogEntity savedEntity = blogRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Blog> findById(Long id) {
        return blogRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Blog> findAll(Pageable pageable) {
        return blogRepository.findAll(pageable).map(this::toDomain);
    }

    @Override
    public List<Blog> findActiveBlogs() {
        return blogRepository.findActiveBlogs()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Blog> findByCompany(String company) {
        return blogRepository.findByCompanyContainingIgnoreCase(company)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Blog> findBlogsNeedingCrawl() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return blogRepository.findByStatus(BlogStatus.ACTIVE)
                .stream()
                .filter(entity -> entity.getLastCrawledAt() == null || 
                               entity.getLastCrawledAt().isBefore(oneHourAgo))
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Blog toDomain(BlogEntity entity) {
        return Blog.builder()
                .id(entity.getId())
                .name(entity.getName())
                .company(entity.getCompany())
                .rssUrl(entity.getRssUrl())
                .siteUrl(entity.getSiteUrl())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .lastCrawledAt(entity.getLastCrawledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private BlogEntity toEntity(Blog domain) {
        return BlogEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .company(domain.getCompany())
                .rssUrl(domain.getRssUrl())
                .siteUrl(domain.getSiteUrl())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .lastCrawledAt(domain.getLastCrawledAt())
                .build();
    }
}
