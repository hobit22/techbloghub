package com.techbloghub.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 배치 작업 설정
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.crawler.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class BatchConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 스케줄링 작업을 위한 별도 스레드 풀 설정
        taskRegistrar.setScheduler(taskExecutor());
        log.info("RSS crawling scheduler configured with dedicated thread pool");
    }

    /**
     * 스케줄링 작업용 스레드 풀
     * RSS 크롤링은 I/O 집약적이므로 별도 스레드에서 실행
     */
    private Executor taskExecutor() {
        return Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "rss-crawler-scheduler");
            thread.setDaemon(false);
            return thread;
        });
    }
}