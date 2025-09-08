package com.techbloghub.persistance.adapter;

import com.techbloghub.domain.model.Tag;
import com.techbloghub.domain.port.out.TagRepositoryPort;
import com.techbloghub.persistance.entity.TagEntity;
import com.techbloghub.persistance.repository.TagRepository;
import com.techbloghub.persistance.repository.PostTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Repository
public class TagAdapter implements TagRepositoryPort {

    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;

    @Override
    public Tag save(Tag tag) {
        TagEntity entity = tag.getId() != null ? 
            TagEntity.fromDomainWithId(tag) : 
            TagEntity.fromDomain(tag);
        TagEntity savedEntity = tagRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<Tag> findById(Long id) {
        return tagRepository.findById(id)
                .map(TagEntity::toDomain);
    }

    @Override
    public Optional<Tag> findByName(String name) {
        return tagRepository.findByName(name)
                .map(TagEntity::toDomain);
    }

    @Override
    public List<Tag> findByNamesIn(Set<String> names) {
        return tagRepository.findByNameIn(names).stream()
                .map(TagEntity::toDomain)
                .toList();
    }

    @Override
    public List<Tag> findAll() {
        return tagRepository.findAll().stream()
                .map(TagEntity::toDomain)
                .toList();
    }

    @Override
    public List<Tag> findByNameContaining(String keyword) {
        return tagRepository.findByNameContaining(keyword).stream()
                .map(TagEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        return tagRepository.existsByName(name);
    }

    @Override
    public void deleteById(Long id) {
        tagRepository.deleteById(id);
    }

    @Override
    public List<Tag> findUnusedTags() {
        List<Long> unusedTagIds = postTagRepository.findUnusedTagIds();
        return tagRepository.findAllById(unusedTagIds).stream()
                .map(TagEntity::toDomain)
                .toList();
    }
}