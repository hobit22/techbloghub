package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Tag;
import com.techbloghub.domain.port.in.TagUseCase;
import com.techbloghub.domain.port.out.TagRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService implements TagUseCase {

    private final TagRepositoryPort tagRepositoryPort;

    @Transactional
    public Tag createTag(String name, String description) {
        if (tagRepositoryPort.existsByName(name)) {
            throw new IllegalArgumentException("Tag with name '" + name + "' already exists");
        }
        
        Tag tag = Tag.builder()
                .name(name)
                .description(description)
                .build();
        
        return tagRepositoryPort.save(tag);
    }

    @Transactional
    public Tag updateTag(Long id, String name, String description) {
        Optional<Tag> existingTag = tagRepositoryPort.findById(id);
        if (existingTag.isEmpty()) {
            throw new IllegalArgumentException("Tag with id " + id + " not found");
        }

        Tag tag = Tag.builder()
                .id(id)
                .name(name)
                .description(description)
                .createdAt(existingTag.get().getCreatedAt())
                .updatedAt(existingTag.get().getUpdatedAt())
                .build();

        return tagRepositoryPort.save(tag);
    }

    public Optional<Tag> findById(Long id) {
        return tagRepositoryPort.findById(id);
    }

    public Optional<Tag> findByName(String name) {
        return tagRepositoryPort.findByName(name);
    }

    @Override
    public List<Tag> getAllTags() {
        return tagRepositoryPort.findAll();
    }
    
    @Override
    public List<Tag> searchTags(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllTags();
        }
        return tagRepositoryPort.findByNameContaining(query.trim());
    }
    
    public List<Tag> findAll() {
        return getAllTags();
    }

    @Transactional
    public void deleteTag(Long id) {
        if (!tagRepositoryPort.findById(id).isPresent()) {
            throw new IllegalArgumentException("Tag with id " + id + " not found");
        }
        tagRepositoryPort.deleteById(id);
    }

    public List<Tag> findUnusedTags() {
        return tagRepositoryPort.findUnusedTags();
    }

    @Transactional
    public Set<Tag> getOrCreateTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Set.of();
        }

        Set<Tag> result = new HashSet<>();
        List<Tag> existingTags = tagRepositoryPort.findByNamesIn(tagNames);
        
        Set<String> existingTagNames = new HashSet<>();
        for (Tag existingTag : existingTags) {
            result.add(existingTag);
            existingTagNames.add(existingTag.getName());
        }

        for (String tagName : tagNames) {
            if (!existingTagNames.contains(tagName)) {
                Tag newTag = Tag.builder()
                        .name(tagName)
                        .description("Auto-generated tag")
                        .build();
                Tag savedTag = tagRepositoryPort.save(newTag);
                result.add(savedTag);
            }
        }

        return result;
    }
}