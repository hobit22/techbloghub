package com.techbloghub.persistence.adapter;

import com.techbloghub.domain.model.Post;
import com.techbloghub.domain.model.SearchCondition;
import com.techbloghub.domain.model.TaggingProcessStatus;
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

import java.util.List;
import java.util.Optional;

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

    @Override
    public void updateTaggingStatus(Long postId, TaggingProcessStatus status) {
        PostEntity entity = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

        postRepository.updateTaggingProcessStatus(postId, status);
        log.debug("Updated tagging status for post {}: {}", postId, status);
    }

    @Override
    public Optional<Post> findById(Long postId) {
        return postRepository.findById(postId).map(PostEntity::toDomain);
    }

    @Override
    public List<Post> findByTaggingStatus(TaggingProcessStatus taggingProcessStatus, int limit) {
        return postRepository.findByTaggingStatus(taggingProcessStatus, limit)
                .stream()
                .map(PostEntity::toDomain)
                .toList();
    }

}