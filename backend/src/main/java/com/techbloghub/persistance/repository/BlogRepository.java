package com.techbloghub.persistance.repository;

import com.techbloghub.domain.model.BlogStatus;
import com.techbloghub.persistance.entity.BlogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<BlogEntity, Long> {

    Optional<BlogEntity> findByRssUrl(String rssUrl);

    List<BlogEntity> findByStatus(BlogStatus status);

    List<BlogEntity> findByCompanyContainingIgnoreCase(String company);

    @Query("SELECT b FROM BlogEntity b WHERE b.status = 'ACTIVE' ORDER BY b.name ASC")
    List<BlogEntity> findActiveBlogs();

    boolean existsByRssUrl(String rssUrl);
}