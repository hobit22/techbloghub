package com.techbloghub.batch;

import com.techbloghub.domain.port.in.CrawlerUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RSS 크롤링 스케줄러
 * 헥사고날 아키텍처에서 Application Layer에 위치
 * 스케줄링 트리거 역할만 수행하고, 실제 비즈니스 로직은 Domain Service에 위임
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlerScheduler {

    private final CrawlerUseCase crawlerUseCase;

    /**
     * 매시간마다 모든 활성 블로그를 크롤링
     * cron: 매시간 0분에 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleHourlyCrawling() {
        log.info("Starting scheduled hourly RSS crawling");
        
        try {
            crawlerUseCase.crawlAllActiveBlogs();
            log.info("Scheduled hourly RSS crawling completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled hourly RSS crawling", e);
        }
    }

    /**
     * 매일 오전 6시에 전체 크롤링 수행
     * cron: 매일 오전 6시 0분에 실행
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void scheduleDailyCrawling() {
        log.info("Starting scheduled daily RSS crawling");
        
        try {
            crawlerUseCase.crawlAllActiveBlogs();
            log.info("Scheduled daily RSS crawling completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled daily RSS crawling", e);
        }
    }
}