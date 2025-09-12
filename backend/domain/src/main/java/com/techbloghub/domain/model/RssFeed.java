package com.techbloghub.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RSS 피드 도메인 모델
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class RssFeed {

    private final String url;
    private final String title;
    private final String description;
    private final LocalDateTime lastBuildDate;
    private final List<RssEntry> entries;

    /**
     * RSS 피드가 유효한지 검증
     */
    public boolean isValid() {
        return url != null && 
               !url.trim().isEmpty() && 
               entries != null && 
               !entries.isEmpty();
    }

    /**
     * 지정된 개수만큼의 유효한 엔트리 반환
     */
    public List<RssEntry> getValidEntries(int maxCount) {
        if (entries == null) return List.of();
        return entries.stream()
                .filter(RssEntry::isValid)
                .limit(maxCount)
                .toList();
    }
}