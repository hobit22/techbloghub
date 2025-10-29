package com.techbloghub.output.discord.listener;

import com.techbloghub.domain.crawling.event.CrawlingCompletedEvent;
import com.techbloghub.output.discord.client.DiscordWebhookClient;
import com.techbloghub.output.discord.config.DiscordConfig;
import com.techbloghub.output.discord.formatter.DiscordMessageFormatter;
import com.techbloghub.output.discord.model.DiscordWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 크롤링 완료 이벤트 리스너
 * CrawlingCompletedEvent를 수신하여 Discord 알림 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "discord.webhook.enabled", havingValue = "true")
public class CrawlingEventListener {

    private final DiscordWebhookClient webhookClient;
    private final DiscordMessageFormatter messageFormatter;
    private final DiscordConfig discordConfig;

    /**
     * 크롤링 완료 이벤트 처리
     * 비동기로 실행되어 크롤링 메인 로직을 블로킹하지 않음
     */
    @EventListener
    @Async
    public void handleCrawlingCompleted(CrawlingCompletedEvent event) {
        log.info("Received CrawlingCompletedEvent at {}", event.getOccurredAt());

        // Discord 설정 확인
        if (!discordConfig.isConfigured()) {
            log.debug("Discord webhook not configured, skipping notification");
            return;
        }

        // 알림 필요 여부 체크 (새 글이 있거나 에러가 있는 경우만)
        if (!event.shouldNotify()) {
            log.debug("No notification needed - no new posts and no errors");
            return;
        }

        try {
            // 메시지 포맷팅
            DiscordWebhookRequest request = messageFormatter.format(event.getResult());

            // Discord 전송
            webhookClient.sendWebhook(request);

            log.info("Discord notification sent successfully for crawling result");

        } catch (Exception e) {
            log.error("Failed to send Discord notification: {}", e.getMessage(), e);
            // 에러가 발생해도 크롤링 프로세스에는 영향을 주지 않음
        }
    }
}
