package com.techbloghub.domain.tagging.auto.service;

import com.techbloghub.domain.tagging.manual.port.CategoryRepositoryPort;
import com.techbloghub.domain.tagging.auto.port.LlmTaggerPort;
import com.techbloghub.domain.tagging.auto.port.TaggingProcessorPort;
import com.techbloghub.domain.tagging.manual.port.TagRepositoryPort;
import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.manual.model.Category;
import com.techbloghub.domain.tagging.manual.model.Tag;
import com.techbloghub.domain.tagging.auto.model.TaggingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaggingProcessor implements TaggingProcessorPort {

    private final LlmTaggerPort llmTaggerPort;
    private final TagRepositoryPort tagRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;

    public TaggingResult processTagging(Post post) {
        log.debug("Starting LLM tagging for post: {}", post.getTitle());

        List<String> existingCategories = getExistingCategories();
        Map<String, List<String>> groupedTags = getGroupedTags();

        return llmTaggerPort.tagContent(
                post.getTitle(),
                post.getContent(),
                groupedTags,
                existingCategories
        );
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
}