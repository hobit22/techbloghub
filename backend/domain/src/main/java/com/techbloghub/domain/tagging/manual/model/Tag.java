package com.techbloghub.domain.tagging.manual.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 태그 도메인 모델
 * 순수한 비즈니스 로직을 담고 있으며, 인프라스트럭처에 의존하지 않음
 * post_tags 중간 테이블을 알지 못함 (헥사고날 아키텍처 원칙)
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "name"})
public class Tag {
    
    private final Long id;
    private final String name;
    private final String description;
    private final String tagGroup;  // 태그 그룹 (language, frontend-framework, etc.)
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

}