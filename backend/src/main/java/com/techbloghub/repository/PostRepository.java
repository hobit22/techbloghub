package com.techbloghub.repository;

import com.techbloghub.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    Optional<Post> findByOriginalUrl(String originalUrl);

    Page<Post> findByBlogIdOrderByPublishedAtDesc(Long blogId, Pageable pageable);

    boolean existsByOriginalUrl(String originalUrl);

    long countByBlogId(Long blogId);
}