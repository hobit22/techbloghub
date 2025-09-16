package com.techbloghub.persistence.repository;

import com.techbloghub.persistence.entity.PostTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTagEntity, Long> {

    /**
     * Check if post-tag relationship exists
     */
    boolean existsByPostIdAndTagId(Long postId, Long tagId);

    /**
     * Find all post-tag relationships by post ID
     */
    List<PostTagEntity> findByPostId(Long postId);

    /**
     * Find all post-tag relationships by tag ID
     */
    List<PostTagEntity> findByTagId(Long tagId);

    /**
     * Delete all post-tag relationships for a post
     */
    void deleteByPostId(Long postId);

    /**
     * Delete all post-tag relationships for a tag
     */
    void deleteByTagId(Long tagId);

    /**
     * Delete specific post-tag relationship
     */
    void deleteByPostIdAndTagId(Long postId, Long tagId);
}