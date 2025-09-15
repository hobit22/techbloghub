package com.techbloghub.batch;


import com.techbloghub.domain.model.TaggingProcessStatus;
import com.techbloghub.domain.model.TaggingResult;
import com.techbloghub.domain.port.in.AutoTaggingUseCase;
import com.techbloghub.domain.port.out.PostRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.autotag.enabled", havingValue = "true", matchIfMissing = false)
public class AutoTaggingScheduler {

    private final AutoTaggingUseCase autoTaggingUseCase;
    private final PostRepositoryPort postRepositoryPort;

    @Value("${app.autotag.batch-size:10}")
    private int batchSize;

    /**
     * 설정된 스케줄에 따라 자동 태깅 배치 작업 실행
     */
    @Scheduled(cron = "${app.autotag.schedule:0 0 * * * ?}")
    public void autoTagging() {
        log.info("Starting auto-tagging batch job (batch size: {})", batchSize);
        long startTime = System.currentTimeMillis();

        try {
            List<TaggingResult> results = autoTaggingUseCase.autoTagUnprocessedPosts(batchSize);

            // 태깅 결과 통계 계산
            int totalTags = results.stream().mapToInt(r -> r.tags().size()).sum();
            int totalCategories = results.stream().mapToInt(r -> r.categories().size()).sum();
            int totalRejectedTags = results.stream().mapToInt(r -> r.rejectedTags().size()).sum();
            int totalRejectedCategories = results.stream().mapToInt(r -> r.rejectedCategories().size()).sum();

            long duration = System.currentTimeMillis() - startTime;

            log.info("Auto-tagging batch job completed successfully in {}ms: {} posts processed, {} tags assigned, {} categories assigned, {} rejected tags, {} rejected categories",
                    duration, results.size(), totalTags, totalCategories, totalRejectedTags, totalRejectedCategories);

            // 전체 태깅 상태 통계 로깅
            logTaggingStatistics();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Auto-tagging batch job failed after {}ms: {}", duration, e.getMessage(), e);

            // TODO: 실패 시 메트릭 수집이나 알림 발송 로직 추가 고려
        }
    }

    /**
     * 전체 포스트의 태깅 상태 통계를 로깅
     */
    private void logTaggingStatistics() {
        try {
            Map<TaggingProcessStatus, Long> statistics = postRepositoryPort.getTaggingStatusStatistics();

            long totalPosts = statistics.values().stream().mapToLong(Long::longValue).sum();

            log.info("=== Tagging Status Statistics ===");
            log.info("Total posts: {}", totalPosts);

            // 각 상태별 통계 로깅
            for (TaggingProcessStatus status : TaggingProcessStatus.values()) {
                Long count = statistics.getOrDefault(status, 0L);
                double percentage = totalPosts > 0 ? (count * 100.0 / totalPosts) : 0.0;
                log.info("{}: {} posts ({:.1f}%)", status, count, percentage);
            }

            log.info("=== End Statistics ===");

        } catch (Exception e) {
            log.warn("Failed to log tagging statistics: {}", e.getMessage());
        }
    }
}
