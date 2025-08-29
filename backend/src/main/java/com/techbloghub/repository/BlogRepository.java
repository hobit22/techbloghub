package com.techbloghub.repository;

import com.techbloghub.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    Optional<Blog> findByRssUrl(String rssUrl);

    List<Blog> findByStatus(Blog.BlogStatus status);

    List<Blog> findByCompanyContainingIgnoreCase(String company);

    @Query("SELECT b FROM Blog b WHERE b.status = 'ACTIVE' ORDER BY b.name ASC")
    List<Blog> findActiveBlogs();

    boolean existsByRssUrl(String rssUrl);
}