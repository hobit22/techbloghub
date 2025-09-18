package com.techbloghub.batch;

import com.techbloghub.domain.crawling.model.CrawlingResult;
import com.techbloghub.domain.crawling.usecase.CrawlRssUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RSS 크롤링 스케줄러
 * Spring의 @Scheduled를 사용한 배치 작업
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.crawler.enabled", havingValue = "true", matchIfMissing = false)
public class RssCrawlerScheduler {

    private final CrawlRssUseCase crawlRssUseCase;

    /**
     * 매일 오전 6시에 모든 활성 블로그 크롤링
     */
    @Scheduled(cron = "${app.crawler.schedule.all-blogs:0 0 6 * * ?}")
    public void crawlAllActiveBlogs() {
        log.info("Starting scheduled RSS crawling for all active blogs");

        try {
            CrawlingResult result = crawlRssUseCase.crawlAllActiveBlogs();

            if (result.isSuccessful()) {
                log.info("RSS crawling completed successfully. " +
                                "Processed: {}/{} blogs, Posts saved: {}, Duration: {}s",
                        result.getProcessedBlogs(), result.getTotalBlogs(),
                        result.getTotalPostsSaved(), result.getExecutionTimeInSeconds());

            } else if (result.isPartiallySuccessful()) {
                log.warn("RSS crawling completed with some failures. " +
                                "Processed: {}/{} blogs, Posts saved: {}, Errors: {}, Duration: {}s",
                        result.getProcessedBlogs(), result.getTotalBlogs(),
                        result.getTotalPostsSaved(), result.getErrors().size(),
                        result.getExecutionTimeInSeconds());

            } else {
                log.error("RSS crawling failed completely. " +
                                "Total blogs: {}, Errors: {}, Duration: {}s",
                        result.getTotalBlogs(), result.getErrors().size(),
                        result.getExecutionTimeInSeconds());
            }

        } catch (Exception e) {
            log.error("Fatal error during RSS crawling", e);
        }
    }

}