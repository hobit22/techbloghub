package com.techbloghub.domain.tagging.auto.service;

import com.techbloghub.domain.tagging.auto.port.TagPersistencePort;
import com.techbloghub.domain.tagging.manual.port.CategoryRepositoryPort;
import com.techbloghub.domain.post.port.PostCategoryRepositoryPort;
import com.techbloghub.domain.post.port.PostTagRepositoryPort;
import com.techbloghub.domain.tagging.manual.port.TagRepositoryPort;
import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.manual.model.Category;
import com.techbloghub.domain.tagging.manual.model.Tag;
import com.techbloghub.domain.tagging.auto.model.TaggingProcessStatus;
import com.techbloghub.domain.tagging.auto.model.TaggingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TagPersistenceService implements TagPersistencePort {

    private final TagRepositoryPort tagRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final PostTagRepositoryPort postTagRepositoryPort;
    private final PostCategoryRepositoryPort postCategoryRepositoryPort;

    public TaggingProcessStatus persistTaggingResult(Post post, TaggingResult result) {
        boolean hasValidTags = !result.tags().isEmpty();
        boolean hasValidCategories = !result.categories().isEmpty();

        if (hasValidTags) {
            saveTagRelations(post.getId(), result.tags());
            log.debug("Assigned {} tags to post {}", result.tags().size(), post.getId());
        }

        if (hasValidCategories) {
            saveCategoryRelations(post.getId(), result.categories());
            log.debug("Assigned {} categories to post {}", result.categories().size(), post.getId());
        }

        return determineStatus(hasValidTags, hasValidCategories);
    }

    private void saveTagRelations(Long postId, List<String> tagNames) {
        List<Tag> tagObjects = convertStringToTags(tagNames);
        for (Tag tag : tagObjects) {
            postTagRepositoryPort.save(postId, tag.getId());
        }
    }

    private void saveCategoryRelations(Long postId, List<String> categoryNames) {
        List<Category> categoryObjects = convertStringToCategories(categoryNames);
        for (Category category : categoryObjects) {
            postCategoryRepositoryPort.save(postId, category.getId());
        }
    }

    private List<Tag> convertStringToTags(List<String> tagNames) {
        return tagNames.stream()
                .map(name -> tagRepositoryPort.findByName(name)
                        .orElseGet(() -> tagRepositoryPort.save(Tag.builder()
                                .name(name)
                                .build())))
                .toList();
    }

    private List<Category> convertStringToCategories(List<String> categoryNames) {
        return categoryNames.stream()
                .map(name -> categoryRepositoryPort.findByName(name)
                        .orElseGet(() -> categoryRepositoryPort.save(Category.builder()
                                .name(name)
                                .build())))
                .toList();
    }

    private TaggingProcessStatus determineStatus(boolean hasValidTags, boolean hasValidCategories) {
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
}