package com.techbloghub.persistence.adapter;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import com.techbloghub.persistence.entity.PostEntity;
import com.techbloghub.persistence.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Repository
@Slf4j
@Transactional
public class PostAdapter implements PostRepositoryPort {

    private final PostRepository postRepository;

    @Override
    public Page<Post> searchPosts(SearchCondition searchCondition, Pageable pageable) {
        return postRepository.searchPosts(searchCondition, pageable)
                .map(PostEntity::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long savePost(Post post) {
        if (!post.isValid()) {
            throw new IllegalArgumentException("Invalid post: " + post.getTitle());
        }

        try {
            PostEntity postEntity = PostEntity.fromDomain(post);

            PostEntity savedEntity = postRepository.save(postEntity);

            log.debug("Saved post: {} [ID: {}, Blog: {}]",
                    post.getTitle(), savedEntity.getId(), post.getBlog().getId());

            return savedEntity.getId();

        } catch (Exception e) {
            log.error("Failed to save post: {} from blog: {} - {}",
                    post.getTitle(), post.getBlog().getId(), e.getMessage());
            throw new RuntimeException("Failed to save post", e);
        }
    }

    @Override
    public boolean existsByNormalizedUrl(String normalizedUrl) {
        if (normalizedUrl == null || normalizedUrl.trim().isEmpty()) {
            return false;
        }
        return postRepository.existsByNormalizedUrl(normalizedUrl);
    }

}