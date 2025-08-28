package com.techbloghub.service;

import com.techbloghub.entity.Category;
import com.techbloghub.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
        "Frontend", Arrays.asList("react", "vue", "angular", "javascript", "typescript", "html", "css", "ui", "ux", "프론트엔드", "사용자", "인터페이스"),
        "Backend", Arrays.asList("spring", "java", "python", "nodejs", "api", "server", "database", "백엔드", "서버", "데이터베이스", "API"),
        "DevOps", Arrays.asList("docker", "kubernetes", "aws", "gcp", "azure", "ci/cd", "jenkins", "배포", "인프라", "클라우드", "컨테이너", "devops"),
        "Data", Arrays.asList("bigdata", "analytics", "ml", "ai", "data", "spark", "hadoop", "데이터", "분석", "머신러닝", "인공지능", "빅데이터"),
        "Mobile", Arrays.asList("android", "ios", "flutter", "react-native", "mobile", "앱", "모바일", "안드로이드"),
        "Security", Arrays.asList("security", "auth", "encryption", "보안", "인증", "암호화", "해킹"),
        "Performance", Arrays.asList("performance", "optimization", "caching", "성능", "최적화", "캐시", "속도"),
        "Architecture", Arrays.asList("architecture", "design", "pattern", "microservice", "아키텍처", "설계", "패턴", "마이크로서비스")
    );
    
    public Set<Category> extractAndCreateCategories(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        String lowerContent = content.toLowerCase();
        Set<String> extractedCategories = new HashSet<>();
        
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            String categoryName = entry.getKey();
            List<String> keywords = entry.getValue();
            
            boolean hasKeyword = keywords.stream()
                    .anyMatch(keyword -> lowerContent.contains(keyword.toLowerCase()));
            
            if (hasKeyword) {
                extractedCategories.add(categoryName);
            }
        }
        
        if (extractedCategories.isEmpty()) {
            extractedCategories.add("General");
        }
        
        return extractedCategories.stream()
                .limit(3)
                .map(this::findOrCreateCategory)
                .collect(Collectors.toSet());
    }
    
    private Category findOrCreateCategory(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseGet(() -> {
                    Category newCategory = Category.builder()
                            .name(categoryName)
                            .description("Auto-generated category")
                            .color(getRandomColor())
                            .build();
                    return categoryRepository.save(newCategory);
                });
    }
    
    private String getRandomColor() {
        String[] colors = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FECA57", "#FF9FF3", "#54A0FF", "#5F27CD"};
        return colors[new Random().nextInt(colors.length)];
    }
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    public Category createCategory(String name, String description, String color) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Category already exists: " + name);
        }
        
        Category category = Category.builder()
                .name(name)
                .description(description)
                .color(color)
                .build();
        
        return categoryRepository.save(category);
    }
}