package com.techbloghub.persistence.adapter;

import com.techbloghub.domain.model.Tag;
import com.techbloghub.domain.port.out.TagRepositoryPort;
import com.techbloghub.persistence.entity.TagEntity;
import com.techbloghub.persistence.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class TagAdapter implements TagRepositoryPort {

    private final TagRepository tagRepository;

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
    public Optional<Tag> findByName(String name) {
        return tagRepository.findByName(name)
                .map(TagEntity::toDomain);
    }

    @Override
    public Tag save(Tag tag) {
        TagEntity entity = TagEntity.fromDomain(tag);
        TagEntity savedEntity = tagRepository.save(entity);
        return savedEntity.toDomain();
    }
}