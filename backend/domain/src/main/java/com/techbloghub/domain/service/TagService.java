package com.techbloghub.domain.service;

import com.techbloghub.domain.model.Tag;
import com.techbloghub.domain.port.in.TagUseCase;
import com.techbloghub.domain.port.out.TagRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TagService implements TagUseCase {

    private final TagRepositoryPort tagRepositoryPort;

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

}