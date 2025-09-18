package com.techbloghub.domain.tagging.auto.service;

import com.techbloghub.domain.tagging.auto.port.RejectedItemPort;
import com.techbloghub.domain.tagging.auto.port.TagPersistencePort;
import com.techbloghub.domain.tagging.auto.port.TaggingProcessorPort;
import com.techbloghub.domain.tagging.auto.usecase.AutoTaggingUseCase;
import com.techbloghub.domain.post.port.PostRepositoryPort;
import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.auto.model.TaggingProcessStatus;
import com.techbloghub.domain.tagging.auto.model.TaggingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AutoTaggingService implements AutoTaggingUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final TaggingProcessorPort taggingProcessor;
    private final TagPersistencePort tagPersistenceService;
    private final RejectedItemPort rejectedItemService;

    @Override
    public TaggingResult autoTag(Long postId) {
        Post post = postRepositoryPort.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

        try {
            TaggingResult result = taggingProcessor.processTagging(post);
            TaggingProcessStatus newStatus = tagPersistenceService.persistTaggingResult(post, result);
            postRepositoryPort.updateTaggingStatus(post.getId(), newStatus);
            rejectedItemService.saveRejectedItems(post, result);

            log.info("LLM tagging completed for post {}: {} tags, {} categories, {} rejected tags, {} rejected categories, status: {}",
                    post.getId(),
                    result.tags().size(),
                    result.categories().size(),
                    result.rejectedTags().size(),
                    result.rejectedCategories().size(),
                    newStatus);

            return result;
        } catch (Exception e) {
            postRepositoryPort.updateTaggingStatus(post.getId(), TaggingProcessStatus.FAILED);
            throw e;
        }
    }

    @Override
    public List<TaggingResult> autoTagUnprocessedPosts(int count) {
        List<Post> postList = postRepositoryPort.findByTaggingStatus(TaggingProcessStatus.NOT_PROCESSED, count);

        return postList.stream()
                .map(post -> {
                    TaggingResult result = taggingProcessor.processTagging(post);
                    TaggingProcessStatus newStatus = tagPersistenceService.persistTaggingResult(post, result);
                    postRepositoryPort.updateTaggingStatus(post.getId(), newStatus);
                    rejectedItemService.saveRejectedItems(post, result);
                    return result;
                })
                .toList();
    }

}