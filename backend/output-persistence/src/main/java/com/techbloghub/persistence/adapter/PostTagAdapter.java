package com.techbloghub.persistence.adapter;

import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.manual.model.Tag;
import com.techbloghub.domain.post.port.PostTagRepositoryPort;
import com.techbloghub.persistence.entity.PostTagEntity;
import com.techbloghub.persistence.repository.PostTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Repository
@Slf4j
@Transactional
public class PostTagAdapter implements PostTagRepositoryPort {

    private final PostTagRepository postTagRepository;

    @Override
    public void save(Long postId, Long tagId) {
        if (postTagRepository.existsByPostIdAndTagId(postId, tagId)) {
            log.debug("Post-Tag relationship already exists: postId={}, tagId={}", postId, tagId);
            return;
        }
        
        PostTagEntity entity = PostTagEntity.create(postId, tagId);
        postTagRepository.save(entity);
        log.debug("Saved Post-Tag relationship: postId={}, tagId={}", postId, tagId);
    }

    @Override
    public void assignTagsToPost(Post post, List<Tag> tags) {
        for (Tag tag : tags) {
            save(post.getId(), tag.getId());
        }
    }

    @Override
    public void removeAllTagsFromPost(Long postId) {
        postTagRepository.deleteByPostId(postId);
        log.debug("Removed all tag relationships for post: {}", postId);
    }

    @Override
    public List<Tag> findTagsByPostId(Long postId) {
        return postTagRepository.findByPostId(postId)
                .stream()
                .map(postTagEntity -> postTagEntity.getTag().toDomain())
                .toList();
    }
}