package com.techbloghub.domain.crawling.event;

import com.techbloghub.domain.crawling.model.CrawlingResult;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 크롤링 완료 도메인 이벤트
 * 순수 도메인 이벤트로 Spring Framework 의존성 없음
 */
@Getter
public class CrawlingCompletedEvent {

    private final CrawlingResult result;
    private final LocalDateTime occurredAt;

    public CrawlingCompletedEvent(CrawlingResult result, LocalDateTime occurredAt) {
        this.result = result;
        this.occurredAt = occurredAt;
    }

    /**
     * 알림이 필요한 이벤트인지 확인
     * 새로운 글이 저장되었거나 에러가 발생한 경우 알림 필요
     */
    public boolean shouldNotify() {
        return result.getTotalPostsSaved() > 0 || !result.getErrors().isEmpty();
    }
}
