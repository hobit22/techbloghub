package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.model.Post;

import java.util.List;

/**
 * Post-Category relationship repository outbound port
 */
public interface PostCategoryRepositoryPort {
    
    /**
     * Save post-category relationship
     * @param postId ID of the post
     * @param categoryId ID of the category
     */
    void save(Long postId, Long categoryId);
    
    /**
     * Assign multiple categories to a post
     *
     * @param post 포스트
     * @param categories 연결할 카테고리 목록
     */
    void assignCategoriesToPost(Post post, List<Category> categories);

    /**
     * Remove all category connections from post
     *
     * @param postId 포스트 ID
     */
    void removeAllCategoriesFromPost(Long postId);

    /**
     * Find categories connected to post
     *
     * @param postId 포스트 ID
     * @return 연결된 카테고리 목록
     */
    List<Category> findCategoriesByPostId(Long postId);
}