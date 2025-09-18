package com.techbloghub.persistence.adapter;

import com.techbloghub.domain.tagging.auto.port.RejectedTagRepositoryPort;
import com.techbloghub.persistence.entity.PostEntity;
import com.techbloghub.persistence.entity.RejectedTagEntity;
import com.techbloghub.persistence.repository.PostRepository;
import com.techbloghub.persistence.repository.RejectedTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain Port를 구현하는 RejectedTag Repository 어댑터
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RejectedTagPortAdapter implements RejectedTagRepositoryPort {

    private final RejectedTagRepository rejectedTagRepository;
    private final PostRepository postRepository;

    @Override
    @Transactional
    public void saveRejectedTag(Long postId, String tagName, String reason) {
        try {
            PostEntity post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

            var existing = rejectedTagRepository.findByTagNameAndPost(tagName, post);

            if (existing.isPresent()) {
                // 이미 존재하면 빈도 증가
                existing.get().incrementFrequency();
                log.debug("Incremented frequency for rejected tag '{}' to {}", 
                    tagName, existing.get().getFrequencyCount());
            } else {
                // 새로 생성
                var rejectedTag = new RejectedTagEntity(tagName, post, reason);
                rejectedTagRepository.save(rejectedTag);
                log.debug("Saved new rejected tag '{}' for post {} with reason: {}", 
                    tagName, postId, reason);
            }

        } catch (Exception e) {
            log.error("Error saving rejected tag '{}' for post {}: {}", tagName, postId, e.getMessage());
            throw new RuntimeException("Failed to save rejected tag", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllRejectedTags() {
        try {
            return rejectedTagRepository.findAll()
                    .stream()
                    .map(RejectedTagEntity::getTagName)
                    .distinct()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding all rejected tags: {}", e.getMessage());
            return List.of();
        }
    }
}