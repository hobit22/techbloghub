package com.techbloghub.persistance.adapter;

import com.techbloghub.domain.model.Tag;
import com.techbloghub.domain.port.out.TagRepositoryPort;
import com.techbloghub.persistance.entity.TagEntity;
import com.techbloghub.persistance.repository.TagsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class TagAdapter implements TagRepositoryPort {
    
    private final TagsRepository tagsRepository;
    
    private static final Set<String> TECH_KEYWORDS = Set.of(
            "java", "spring", "javascript", "python", "react", "vue", "angular", "nodejs",
            "docker", "kubernetes", "aws", "azure", "gcp", "mysql", "postgresql", "mongodb",
            "redis", "elasticsearch", "kafka", "rabbitmq", "microservices", "api", "rest",
            "graphql", "git", "devops", "ci/cd", "jenkins", "github", "gitlab", "linux",
            "algorithm", "data-structure", "machine-learning", "ai", "deep-learning",
            "frontend", "backend", "fullstack", "mobile", "android", "ios", "flutter",
            "typescript", "go", "rust", "kotlin", "swift", "c++", "c#", ".net"
    );
    
    @Override
    public Tag save(Tag tag) {
        TagEntity entity = toEntity(tag);
        TagEntity savedEntity = tagsRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Tag> findById(Long id) {
        return tagsRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Tag> findByName(String name) {
        return tagsRepository.findByName(name).map(this::toDomain);
    }

    @Override
    public List<Tag> findAll() {
        return tagsRepository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Tag> extractAndCreateTags(String text) {
        Set<Tag> tags = new HashSet<>();
        
        if (text == null || text.trim().isEmpty()) {
            return tags;
        }
        
        Set<String> extractedTagNames = extractTagNames(text.toLowerCase());
        
        for (String tagName : extractedTagNames) {
            Optional<Tag> existingTag = findByName(tagName);
            if (existingTag.isPresent()) {
                tags.add(existingTag.get());
            } else {
                Tag newTag = Tag.builder()
                        .name(tagName)
                        .description(tagName + " 기술 태그")
                        .build();
                tags.add(save(newTag));
            }
        }
        
        return tags;
    }

    private Set<String> extractTagNames(String text) {
        Set<String> tags = new HashSet<>();
        
        // 기술 키워드 매칭
        for (String keyword : TECH_KEYWORDS) {
            if (text.contains(keyword)) {
                tags.add(normalizeTagName(keyword));
            }
        }
        
        // 해시태그 추출 (#으로 시작하는 태그)
        Pattern hashtagPattern = Pattern.compile("#([a-zA-Z0-9가-힣_]+)");
        hashtagPattern.matcher(text).results()
                .forEach(match -> tags.add(normalizeTagName(match.group(1))));
        
        // 괄호 안의 기술명 추출
        Pattern bracketPattern = Pattern.compile("\\[([a-zA-Z0-9가-힣\\s]+)\\]|\\(([a-zA-Z0-9가-힣\\s]+)\\)");
        bracketPattern.matcher(text).results()
                .forEach(match -> {
                    String group1 = match.group(1);
                    String group2 = match.group(2);
                    String bracketContent = group1 != null ? group1 : group2;
                    if (bracketContent != null && bracketContent.trim().length() > 2 
                        && bracketContent.trim().length() <= 20) {
                        tags.add(normalizeTagName(bracketContent.trim()));
                    }
                });
        
        // 최대 10개의 태그로 제한
        return tags.stream()
                .filter(tag -> tag.length() >= 2 && tag.length() <= 20)
                .limit(10)
                .collect(Collectors.toSet());
    }

    private String normalizeTagName(String tagName) {
        return tagName.toLowerCase()
                .replaceAll("[^a-zA-Z0-9가-힣\\-_]", "")
                .trim();
    }

    private Tag toDomain(TagEntity entity) {
        return Tag.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TagEntity toEntity(Tag domain) {
        return TagEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .build();
    }
}