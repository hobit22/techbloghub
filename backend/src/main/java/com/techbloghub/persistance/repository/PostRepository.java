package com.techbloghub.persistance.repository;

import com.techbloghub.persistance.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long>, PostRepositoryCustom {

    Optional<PostEntity> findByOriginalUrl(String originalUrl);

    Page<PostEntity> findByBlogIdOrderByPublishedAtDesc(Long blogId, Pageable pageable);

    boolean existsByOriginalUrl(String originalUrl);

    long countByBlogId(Long blogId);
}