package com.techbloghub.domain.tagging.auto.service;

import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.auto.model.TaggingResult;
import com.techbloghub.domain.tagging.auto.port.RejectedCategoryRepositoryPort;
import com.techbloghub.domain.tagging.auto.port.RejectedItemPort;
import com.techbloghub.domain.tagging.auto.port.RejectedTagRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RejectedItemService implements RejectedItemPort {

    private final RejectedTagRepositoryPort rejectedTagRepositoryPort;
    private final RejectedCategoryRepositoryPort rejectedCategoryRepositoryPort;

    public void saveRejectedItems(Post post, TaggingResult result) {
        try {
            saveRejectedTags(post.getId(), result.rejectedTags());
            saveRejectedCategories(post.getId(), result.rejectedCategories());
        } catch (Exception e) {
            log.warn("Failed to save rejected items for post {}: {}", post.getId(), e.getMessage());
        }
    }

    private void saveRejectedTags(Long postId, java.util.List<String> rejectedTags) {
        for (String rejectedTag : rejectedTags) {
            rejectedTagRepositoryPort.saveRejectedTag(
                    postId,
                    rejectedTag,
                    "Not in predefined tags list"
            );
            log.debug("Saved rejected tag '{}' for post {}", rejectedTag, postId);
        }
    }

    private void saveRejectedCategories(Long postId, java.util.List<String> rejectedCategories) {
        for (String rejectedCategory : rejectedCategories) {
            rejectedCategoryRepositoryPort.saveRejectedCategory(
                    postId,
                    rejectedCategory,
                    "Not in predefined categories list"
            );
            log.debug("Saved rejected category '{}' for post {}", rejectedCategory, postId);
        }
    }
}