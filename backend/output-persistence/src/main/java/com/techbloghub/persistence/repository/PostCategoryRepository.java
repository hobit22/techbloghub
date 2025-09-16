package com.techbloghub.persistence.repository;

import com.techbloghub.persistence.entity.PostCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostCategoryRepository extends JpaRepository<PostCategoryEntity, Long> {

    /**
     * Check if post-category relationship exists
     */
    boolean existsByPostIdAndCategoryId(Long postId, Long categoryId);

    /**
     * Find all post-category relationships by post ID
     */
    List<PostCategoryEntity> findByPostId(Long postId);

    /**
     * Find all post-category relationships by category ID
     */
    List<PostCategoryEntity> findByCategoryId(Long categoryId);

    /**
     * Delete all post-category relationships for a post
     */
    void deleteByPostId(Long postId);

    /**
     * Delete all post-category relationships for a category
     */
    void deleteByCategoryId(Long categoryId);

    /**
     * Delete specific post-category relationship
     */
    void deleteByPostIdAndCategoryId(Long postId, Long categoryId);
}