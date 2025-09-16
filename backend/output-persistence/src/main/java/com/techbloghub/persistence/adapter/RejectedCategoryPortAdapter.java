package com.techbloghub.persistence.adapter;

import com.techbloghub.domain.port.out.RejectedCategoryRepositoryPort;
import com.techbloghub.persistence.entity.PostEntity;
import com.techbloghub.persistence.entity.RejectedCategoryEntity;
import com.techbloghub.persistence.repository.PostRepository;
import com.techbloghub.persistence.repository.RejectedCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain Port를 구현하는 RejectedCategory Repository 어댑터
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RejectedCategoryPortAdapter implements RejectedCategoryRepositoryPort {

    private final RejectedCategoryRepository rejectedCategoryRepository;
    private final PostRepository postRepository;

    @Override
    @Transactional
    public void saveRejectedCategory(Long postId, String categoryName, String reason) {
        try {
            PostEntity post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

            var existing = rejectedCategoryRepository.findByCategoryNameAndPost(categoryName, post);

            if (existing.isPresent()) {
                // 이미 존재하면 빈도 증가
                existing.get().incrementFrequency();
                log.debug("Incremented frequency for rejected category '{}' to {}", 
                    categoryName, existing.get().getFrequencyCount());
            } else {
                // 새로 생성
                var rejectedCategory = new RejectedCategoryEntity(categoryName, post, reason);
                rejectedCategoryRepository.save(rejectedCategory);
                log.debug("Saved new rejected category '{}' for post {} with reason: {}", 
                    categoryName, postId, reason);
            }

        } catch (Exception e) {
            log.error("Error saving rejected category '{}' for post {}: {}", categoryName, postId, e.getMessage());
            throw new RuntimeException("Failed to save rejected category", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllRejectedCategories() {
        try {
            return rejectedCategoryRepository.findAll()
                    .stream()
                    .map(RejectedCategoryEntity::getCategoryName)
                    .distinct()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding all rejected categories: {}", e.getMessage());
            return List.of();
        }
    }
}