package com.techbloghub.domain.post.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SearchCondition {
    
    private final String keyword;
    private final List<String> tags;
    private final List<String> categories;
    private final List<Long> blogIds;
    private final LocalDate publishedAfter;
    private final LocalDate publishedBefore;
    private final String sortBy;
    private final Sort.Direction sortDirection;
    
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }
    
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
    
    public boolean hasCategories() {
        return categories != null && !categories.isEmpty();
    }
    
    public boolean hasBlogIds() {
        return blogIds != null && !blogIds.isEmpty();
    }
    
    public boolean hasDateRange() {
        return publishedAfter != null || publishedBefore != null;
    }
}