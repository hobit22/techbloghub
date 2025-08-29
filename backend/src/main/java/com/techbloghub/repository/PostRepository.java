package com.techbloghub.repository;

import com.techbloghub.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByOriginalUrl(String originalUrl);

    Page<Post> findByBlogIdOrderByPublishedAtDesc(Long blogId, Pageable pageable);

    @Query("SELECT p FROM Post p ORDER BY p.publishedAt DESC")
    Page<Post> findAllOrderByPublishedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.publishedAt >= :since ORDER BY p.publishedAt DESC")
    List<Post> findRecentPosts(@Param("since") LocalDateTime since);

    @Query("SELECT p FROM Post p JOIN p.postTags pt JOIN pt.tags t WHERE t.name IN :tagNames ORDER BY p.publishedAt DESC")
    Page<Post> findByTagNames(@Param("tagNames") List<String> tagNames, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.postCategories pc JOIN pc.category c WHERE c.name IN :categoryNames ORDER BY p.publishedAt DESC")
    Page<Post> findByCategoryNames(@Param("categoryNames") List<String> categoryNames, Pageable pageable);

    boolean existsByOriginalUrl(String originalUrl);

    long countByBlogId(Long blogId);
}