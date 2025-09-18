package com.techbloghub.output.gpt.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techbloghub.domain.tagging.auto.model.TaggingResult;
import com.techbloghub.domain.tagging.auto.port.LlmTaggerPort;
import com.techbloghub.output.gpt.template.PromptTemplate;
import com.techbloghub.output.gpt.template.SchemeTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAI API를 사용한 LLM 태깅 어댑터
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiAdapter implements LlmTaggerPort {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;


    @Override
    public TaggingResult tagContent(String title, String content, Map<String, List<String>> existingTagGroups, List<String> existingCategories) {

        // 빈 콘텐츠 검증
        if ((title == null || title.trim().isEmpty()) &&
                (content == null || content.trim().isEmpty())) {
            log.warn("No content to analyze");
            return new TaggingResult(List.of(), List.of(), List.of(), List.of());
        }

        String prompt = PromptTemplate.getTaggingPrompt(title, content, existingTagGroups, existingCategories);
        String scheme = SchemeTemplate.getTaggingScheme();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, scheme))
                .build();

        String output = Optional.of(new Prompt(prompt, options))
                .map(chatModel::call)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AbstractMessage::getText)
                .orElseThrow(() -> new RuntimeException("LLM response is empty"));

        try {
            TaggingResult rawResult = objectMapper.readValue(output, TaggingResult.class);
            
            // 검증 및 정제 과정을 거쳐 최종 결과 생성
            TaggingResult validatedResult = validateAndFilterResult(rawResult, existingTagGroups, existingCategories);
            
            log.debug("LLM tagging completed - valid tags: {}, valid categories: {}, rejected tags: {}, rejected categories: {}",
                    validatedResult.tags().size(),
                    validatedResult.categories().size(), 
                    validatedResult.rejectedTags().size(),
                    validatedResult.rejectedCategories().size());
                    
            return validatedResult;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    /**
     * LLM 결과를 검증하고 거부된 항목들을 분리하는 메서드
     */
    private TaggingResult validateAndFilterResult(TaggingResult rawResult, 
                                                Map<String, List<String>> existingTagGroups, 
                                                List<String> existingCategories) {
        
        // 모든 허용된 태그 목록 생성
        Set<String> allValidTags = existingTagGroups.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        
        // 태그 검증
        List<String> validatedTags = new ArrayList<>();
        List<String> rejectedTags = new ArrayList<>();
        
        for (String tag : rawResult.tags()) {
            if (tag == null || tag.trim().isEmpty()) {
                continue;
            }
            
            String cleanTag = tag.trim();
            
            // 1. 정확한 매치
            if (allValidTags.contains(cleanTag)) {
                validatedTags.add(cleanTag);
            } else {
                // 2. 대소문자 무시 매치
                String matchedTag = findCaseInsensitiveMatch(cleanTag, allValidTags);
                if (matchedTag != null) {
                    validatedTags.add(matchedTag);
                    log.debug("Auto-corrected tag case: '{}' -> '{}'", cleanTag, matchedTag);
                } else {
                    // 3. 변형 매핑 시도
                    String mappedTag = mapTagVariations(cleanTag, allValidTags);
                    if (mappedTag != null) {
                        validatedTags.add(mappedTag);
                        log.debug("Auto-mapped tag variation: '{}' -> '{}'", cleanTag, mappedTag);
                    } else {
                        // 거부된 태그
                        rejectedTags.add(cleanTag);
                        log.warn("Rejected non-predefined tag: '{}'", cleanTag);
                    }
                }
            }
        }
        
        // 중복 제거 및 8개 제한
        validatedTags = validatedTags.stream()
                .distinct()
                .limit(8)
                .collect(Collectors.toList());
        
        // 카테고리 검증
        List<String> validatedCategories = new ArrayList<>();
        List<String> rejectedCategories = new ArrayList<>();
        
        Set<String> validCategoriesSet = new HashSet<>(existingCategories);
        
        for (String category : rawResult.categories()) {
            if (category == null || category.trim().isEmpty()) {
                continue;
            }
            
            String cleanCategory = category.trim();
            
            // 1. 정확한 매치
            if (validCategoriesSet.contains(cleanCategory)) {
                validatedCategories.add(cleanCategory);
            } else {
                // 2. 대소문자 무시 매치
                String matchedCategory = findCaseInsensitiveMatch(cleanCategory, validCategoriesSet);
                if (matchedCategory != null) {
                    validatedCategories.add(matchedCategory);
                    log.debug("Auto-corrected category case: '{}' -> '{}'", cleanCategory, matchedCategory);
                } else {
                    // 거부된 카테고리
                    rejectedCategories.add(cleanCategory);
                    log.warn("Rejected non-predefined category: '{}'", cleanCategory);
                }
            }
        }
        
        // 중복 제거 및 3개 제한
        validatedCategories = validatedCategories.stream()
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
        
        return new TaggingResult(validatedTags, validatedCategories, rejectedTags, rejectedCategories);
    }

    /**
     * 대소문자를 무시하고 매치되는 항목 찾기
     */
    private String findCaseInsensitiveMatch(String target, Set<String> candidates) {
        for (String candidate : candidates) {
            if (target.equalsIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 태그 변형 매핑 (Python의 _map_tag_variations와 동일)
     */
    private String mapTagVariations(String tag, Set<String> validTags) {
        String tagLower = tag.toLowerCase();
        
        // 공통 변형 매핑
        Map<String, String> variations = Map.ofEntries(
            // JavaScript 변형
            Map.entry("js", "JavaScript"),
            Map.entry("javascript", "JavaScript"),
            Map.entry("reactjs", "React"),
            Map.entry("react.js", "React"),
            Map.entry("vuejs", "Vue"),
            Map.entry("vue.js", "Vue"),
            Map.entry("angularjs", "Angular"),
            Map.entry("angular.js", "Angular"),
            Map.entry("nodejs", "Node.js"),
            Map.entry("node", "Node.js"),
            Map.entry("nextjs", "Next.js"),
            Map.entry("nuxtjs", "Nuxt.js"),
            
            // Framework 변형
            Map.entry("django", "Django"),
            Map.entry("flask", "Flask"),
            Map.entry("fastapi", "FastAPI"),
            Map.entry("express.js", "Express"),
            Map.entry("expressjs", "Express"),
            Map.entry("spring-boot", "Spring Boot"),
            Map.entry("springboot", "Spring Boot"),
            
            // Database 변형
            Map.entry("postgres", "PostgreSQL"),
            Map.entry("psql", "PostgreSQL"),
            Map.entry("mysql", "MySQL"),
            Map.entry("mongodb", "MongoDB"),
            Map.entry("mongo", "MongoDB"),
            Map.entry("redis", "Redis"),
            
            // Cloud 변형
            Map.entry("amazon-web-services", "AWS"),
            Map.entry("amazon web services", "AWS"),
            Map.entry("google-cloud", "GCP"),
            Map.entry("google cloud platform", "GCP"),
            Map.entry("microsoft-azure", "Azure"),
            
            // Container 변형
            Map.entry("docker", "Docker"),
            Map.entry("k8s", "Kubernetes"),
            Map.entry("kubernetes", "Kubernetes"),
            
            // Other 변형
            Map.entry("typescript", "TypeScript"),
            Map.entry("ts", "TypeScript"),
            Map.entry("python", "Python"),
            Map.entry("java", "Java"),
            Map.entry("golang", "Go"),
            Map.entry("go-lang", "Go"),
            Map.entry("c++", "C++"),
            Map.entry("cpp", "C++"),
            Map.entry("c#", "C#"),
            Map.entry("csharp", "C#"),
            Map.entry("c-sharp", "C#")
        );
        
        // 직접 매핑 확인
        String mapped = variations.get(tagLower);
        if (mapped != null && validTags.contains(mapped)) {
            return mapped;
        }
        
        // 대소문자 무시 매치 재확인
        return findCaseInsensitiveMatch(tag, validTags);
    }
}