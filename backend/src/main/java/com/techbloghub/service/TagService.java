package com.techbloghub.service;

import com.techbloghub.entity.Tag;
import com.techbloghub.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {
    
    private final TagRepository tagRepository;
    
    private static final List<String> TECH_KEYWORDS = Arrays.asList(
        "java", "javascript", "python", "react", "spring", "nodejs", "typescript",
        "aws", "docker", "kubernetes", "microservice", "api", "database", "sql",
        "frontend", "backend", "devops", "ci/cd", "git", "testing", "performance",
        "security", "ml", "ai", "데이터", "개발", "서버", "클라우드", "프레임워크",
        "라이브러리", "알고리즘", "아키텍처", "설계", "최적화", "배포", "모니터링"
    );
    
    private static final Pattern KOREAN_TECH_PATTERN = Pattern.compile(
        "(개발|프로그래밍|코딩|시스템|서버|데이터베이스|프레임워크|라이브러리|API|클라우드|배포|테스트|최적화|아키텍처|설계|알고리즘|성능|보안|모니터링|인프라|DevOps|CI/CD|컨테이너|마이크로서비스)"
    );
    
    public Set<Tag> extractAndCreateTags(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        Set<String> extractedTags = new HashSet<>();
        
        extractedTags.addAll(extractEnglishTechKeywords(content.toLowerCase()));
        extractedTags.addAll(extractKoreanTechKeywords(content));
        
        return extractedTags.stream()
                .limit(10)
                .map(this::findOrCreateTag)
                .collect(Collectors.toSet());
    }
    
    private Set<String> extractEnglishTechKeywords(String content) {
        return TECH_KEYWORDS.stream()
                .filter(keyword -> content.contains(keyword.toLowerCase()))
                .collect(Collectors.toSet());
    }
    
    private Set<String> extractKoreanTechKeywords(String content) {
        Set<String> koreanTags = new HashSet<>();
        
        var matcher = KOREAN_TECH_PATTERN.matcher(content);
        while (matcher.find()) {
            koreanTags.add(matcher.group(1));
        }
        
        return koreanTags;
    }
    
    private Tag findOrCreateTag(String tagName) {
        return tagRepository.findByName(tagName)
                .orElseGet(() -> {
                    Tag newTag = Tag.builder()
                            .name(tagName)
                            .description("Auto-generated tag")
                            .build();
                    return tagRepository.save(newTag);
                });
    }
    
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }
    
    public Tag createTag(String name, String description) {
        if (tagRepository.existsByName(name)) {
            throw new IllegalArgumentException("Tag already exists: " + name);
        }
        
        Tag tag = Tag.builder()
                .name(name)
                .description(description)
                .build();
        
        return tagRepository.save(tag);
    }
}