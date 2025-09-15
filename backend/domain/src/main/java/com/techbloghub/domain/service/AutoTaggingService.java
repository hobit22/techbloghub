package com.techbloghub.domain.service;

import com.techbloghub.domain.model.*;
import com.techbloghub.domain.port.in.AutoTaggingUseCase;
import com.techbloghub.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AutoTaggingService implements AutoTaggingUseCase {

    private final LlmTaggerPort llmTaggerPort;
    private final PostRepositoryPort postRepositoryPort;
    private final TagRepositoryPort tagRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final PostTagRepositoryPort postTagRepositoryPort;
    private final PostCategoryRepositoryPort postCategoryRepositoryPort;
    private final RejectedTagRepositoryPort rejectedTagRepositoryPort;
    private final RejectedCategoryRepositoryPort rejectedCategoryRepositoryPort;

    @Override
    public TaggingResult autoTag(Long postId) {
        Post post = postRepositoryPort.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

        log.debug("Starting LLM tagging for post: {}", post.getTitle());

        // 카테고리 리스트
        List<String> existingCategories = getExistingCategories();

        // 태그 그룹
        Map<String, List<String>> groupedTags = getGroupedTags();

        // 태깅 받아오기
        TaggingResult result = getTaggingResult(existingCategories, groupedTags, post);

        // 태깅 결과 저장
        TaggingProcessStatus newStatus = saveTaggingResult(post, result);
        postRepositoryPort.updateTaggingStatus(post.getId(), newStatus);

        // 거절된 item 저장
        saveRejectedItems(post, result);

        log.info("LLM tagging completed for post {}: {} tags, {} categories, {} rejected tags, {} rejected categories, status: {}",
                post.getId(),
                result.tags().size(),
                result.categories().size(),
                result.rejectedTags().size(),
                result.rejectedCategories().size(),
                newStatus);

        return result;
    }

    @Override
    public List<TaggingResult> autoTagUnprocessedPosts(int count) {
        List<Post> postList = postRepositoryPort.findByTaggingStatus(TaggingProcessStatus.NOT_PROCESSED, count);

        List<String> existingCategories = getExistingCategories();

        Map<String, List<String>> groupedTags = getGroupedTags();

        return postList.stream()
                .map(post -> {
                    TaggingResult result = getTaggingResult(existingCategories, groupedTags, post);
                    TaggingProcessStatus newStatus = saveTaggingResult(post, result);
                    postRepositoryPort.updateTaggingStatus(post.getId(), newStatus);
                    saveRejectedItems(post, result);
                    return result;
                })
                .toList();
    }

    private List<String> getExistingCategories() {
        return categoryRepositoryPort.findAll()
                .stream()
                .map(Category::getName)
                .toList();
    }

    private Map<String, List<String>> getGroupedTags() {
        return tagRepositoryPort
                .findAll()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Tag::getTagGroup,
                                Collectors.mapping(Tag::getName, Collectors.toList())
                        )
                );
    }

    private TaggingResult getTaggingResult(List<String> existingCategories, Map<String, List<String>> groupedTags, Post post) {
        try {
            log.debug("Existing tag groups: {}, categories count: {}",
                    groupedTags.size(), existingCategories.size());

            return llmTaggerPort.tagContent(
                    post.getTitle(),
                    post.getContent(),
                    groupedTags,
                    existingCategories
            );

        } catch (Exception e) {
            log.error("Error tagging post {} with LLM: {}", post.getId(), e.getMessage());
            postRepositoryPort.updateTaggingStatus(post.getId(), TaggingProcessStatus.FAILED);
            throw new RuntimeException("Failed to tag post: " + post.getId(), e);
        }
    }

    private TaggingProcessStatus saveTaggingResult(Post post, TaggingResult result) {
        boolean hasValidTags = !result.tags().isEmpty();
        boolean hasValidCategories = !result.categories().isEmpty();

        // 태그 저장 (String → Tag 객체 변환 후 관계 저장)
        if (hasValidTags) {
            List<Tag> tagObjects = convertStringToTags(result.tags());
            for (Tag tag : tagObjects) {
                postTagRepositoryPort.save(post.getId(), tag.getId());
            }
            log.debug("Assigned {} tags to post {}", tagObjects.size(), post.getId());
        }

        // 카테고리 저장 (String → Category 객체 변환 후 관계 저장)
        if (hasValidCategories) {
            List<Category> categoryObjects = convertStringToCategories(result.categories());
            for (Category category : categoryObjects) {
                postCategoryRepositoryPort.save(post.getId(), category.getId());
            }
            log.debug("Assigned {} categories to post {}", categoryObjects.size(), post.getId());
        }

        // 상태 결정
        if (hasValidTags && hasValidCategories) {
            return TaggingProcessStatus.TAGGED_AND_CATEGORIZED;
        } else if (hasValidTags) {
            return TaggingProcessStatus.TAGGED;
        } else if (hasValidCategories) {
            return TaggingProcessStatus.CATEGORIZED;
        } else {
            return TaggingProcessStatus.NOT_PROCESSED;
        }
    }

    /**
     * 문자열 태그 목록을 Tag 객체 목록으로 변환
     */
    private List<Tag> convertStringToTags(List<String> tagNames) {
        return tagNames.stream()
                .map(name -> tagRepositoryPort.findByName(name)
                        .orElseGet(() -> tagRepositoryPort.save(Tag.builder()
                                .name(name)
                                .build())))
                .toList();
    }

    /**
     * 문자열 카테고리 목록을 Category 객체 목록으로 변환
     */
    private List<Category> convertStringToCategories(List<String> categoryNames) {
        return categoryNames.stream()
                .map(name -> categoryRepositoryPort.findByName(name)
                        .orElseGet(() -> categoryRepositoryPort.save(Category.builder()
                                .name(name)
                                .build())))
                .toList();
    }

    /**
     * 거부된 태그와 카테고리를 저장
     */
    private void saveRejectedItems(Post post, TaggingResult result) {
        try {
            // 거부된 태그 저장
            for (String rejectedTag : result.rejectedTags()) {
                rejectedTagRepositoryPort.saveRejectedTag(
                        post.getId(),
                        rejectedTag,
                        "Not in predefined tags list"
                );
                log.debug("Saved rejected tag '{}' for post {}", rejectedTag, post.getId());
            }

            // 거부된 카테고리 저장
            for (String rejectedCategory : result.rejectedCategories()) {
                rejectedCategoryRepositoryPort.saveRejectedCategory(
                        post.getId(),
                        rejectedCategory,
                        "Not in predefined categories list"
                );
                log.debug("Saved rejected category '{}' for post {}", rejectedCategory, post.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to save rejected items for post {}: {}", post.getId(), e.getMessage());
            // 거부된 항목 저장 실패는 전체 태깅 프로세스를 중단시키지 않음
        }
    }
}