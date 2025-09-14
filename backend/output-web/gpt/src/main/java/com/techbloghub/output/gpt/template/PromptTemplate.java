package com.techbloghub.output.gpt.template;


import com.techbloghub.output.gpt.model.TagDefinitions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromptTemplate {

    /**
     * 태깅을 위한 프롬프트 생성
     */
    public static String getTaggingPrompt(String title, String content, Map<String, List<String>> tags, List<String> categories) {

        return String.format("""
            Please analyze this tech blog post and extract relevant tags and categories.
            
            Title: %s
            
            Content: %s
            
            AVAILABLE TAGS (MUST USE ONLY FROM THIS LIST):
            %s
            
            AVAILABLE CATEGORIES (MUST USE ONLY FROM THIS LIST):
            %s
            
            STRICT GUIDELINES:
            - **MANDATORY**: ONLY use tags from the "AVAILABLE TAGS" list above - DO NOT create new tags
            - **MANDATORY**: ONLY use categories from the "AVAILABLE CATEGORIES" list above 
            - Provide 0-8 most relevant tags that best represent the technical content
            - Provide 1-3 most relevant categories that best fit the post
            - Use EXACT naming from predefined lists (case-sensitive)
            - If the post is in Korean, provide English tags and categories from the lists
            - Focus on technical aspects and concrete technologies mentioned in the content
            - If you cannot find appropriate tags from the predefined list, use fewer tags rather than creating new ones
            """, title, content, tags, categories);
    }

    private static String createTagGroupsString() {
        StringBuilder tagGroupsStr = new StringBuilder();
        TagDefinitions.PREDEFINED_TAGS.forEach((groupName, tags) -> {
            String groupDisplayName = Arrays.stream(groupName.split("-"))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                    .collect(Collectors.joining(" "));
            tagGroupsStr.append(groupDisplayName).append(": ")
                    .append(String.join(", ", tags)).append("\n");
        });
        return tagGroupsStr.toString();
    }
}
