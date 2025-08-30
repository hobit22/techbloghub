package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Tag;
import com.techbloghub.domain.port.in.TagUseCase;
import com.techbloghub.domain.port.out.TagRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 태그 관리 관련 비즈니스 로직을 처리하는 애플리케이션 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TagService implements TagUseCase {

    private final TagRepositoryPort tagRepositoryPort;

    @Override
    public List<Tag> getAllTags() {
        log.debug("Fetching all tags");
        return tagRepositoryPort.findAll();
    }

    @Override
    public Optional<Tag> getTagById(Long id) {
        log.debug("Fetching tag by id: {}", id);
        return tagRepositoryPort.findById(id);
    }

    @Override
    public Optional<Tag> getTagByName(String name) {
        log.debug("Fetching tag by name: {}", name);
        return tagRepositoryPort.findByName(name);
    }

    @Override
    @Transactional
    public Tag saveTag(Tag tag) {
        log.debug("Saving tag: {}", tag.getName());
        
        if (!tag.isValid()) {
            throw new IllegalArgumentException("Invalid tag data");
        }
        
        return tagRepositoryPort.save(tag);
    }
}