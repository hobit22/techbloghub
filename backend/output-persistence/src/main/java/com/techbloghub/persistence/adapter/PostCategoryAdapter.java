package com.techbloghub.persistence.adapter;

import com.techbloghub.domain.model.Category;
import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.port.out.PostCategoryRepositoryPort;
import com.techbloghub.persistence.entity.PostCategoryEntity;
import com.techbloghub.persistence.repository.PostCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Repository
@Slf4j
@Transactional
public class PostCategoryAdapter implements PostCategoryRepositoryPort {

    private final PostCategoryRepository postCategoryRepository;

    @Override
    public void save(Long postId, Long categoryId) {
        if (postCategoryRepository.existsByPostIdAndCategoryId(postId, categoryId)) {
            log.debug("Post-Category relationship already exists: postId={}, categoryId={}", postId, categoryId);
            return;
        }
        
        PostCategoryEntity entity = PostCategoryEntity.create(postId, categoryId);
        postCategoryRepository.save(entity);
        log.debug("Saved Post-Category relationship: postId={}, categoryId={}", postId, categoryId);
    }

    @Override
    public void assignCategoriesToPost(Post post, List<Category> categories) {
        for (Category category : categories) {
            save(post.getId(), category.getId());
        }
    }

    @Override
    public void removeAllCategoriesFromPost(Long postId) {
        postCategoryRepository.deleteByPostId(postId);
        log.debug("Removed all category relationships for post: {}", postId);
    }

    @Override
    public List<Category> findCategoriesByPostId(Long postId) {
        return postCategoryRepository.findByPostId(postId)
                .stream()
                .map(postCategoryEntity -> postCategoryEntity.getCategory().toDomain())
                .toList();
    }
}