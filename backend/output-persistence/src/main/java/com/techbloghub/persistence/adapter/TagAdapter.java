package com.techbloghub.persistence.adapter;

import com.techbloghub.domain.model.Tag;
import com.techbloghub.domain.port.out.TagRepositoryPort;
import com.techbloghub.persistence.entity.TagEntity;
import com.techbloghub.persistence.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}