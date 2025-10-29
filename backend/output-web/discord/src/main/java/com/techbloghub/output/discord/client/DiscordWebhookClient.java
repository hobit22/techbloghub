package com.techbloghub.output.discord.client;

import com.techbloghub.output.discord.config.DiscordConfig;
import com.techbloghub.output.discord.model.DiscordWebhookRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Discord Webhook HTTP 클라이언트
 * WebClient를 사용하여 Discord Webhook API 호출
 */
@Component
@Slf4j
public class DiscordWebhookClient {

    private static final int TIMEOUT_SECONDS = 10;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(2);

    private final WebClient webClient;
    private final DiscordConfig discordConfig;

    public DiscordWebhookClient(DiscordConfig discordConfig) {
        this.discordConfig = discordConfig;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }

    /**
     * Discord Webhook 전송
     */
    public void sendWebhook(DiscordWebhookRequest request) {
        if (!discordConfig.isConfigured()) {
            log.warn("Discord webhook is not configured. Skipping notification.");
            return;
        }

        try {
            webClient.post()
                    .uri(discordConfig.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("Discord API error response: {}", body);
                                        return Mono.error(new RuntimeException("Discord API Error: " + body));
                                    }))
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                            .filter(this::isRetryableException)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                log.error("Discord webhook retry exhausted after {} attempts",
                                        MAX_RETRY_ATTEMPTS);
                                return retrySignal.failure();
                            }))
                    .doOnError(error -> log.error("Failed to send Discord webhook: {}",
                            error.getMessage(), error))
                    .onErrorResume(error -> {
                        // 에러를 로깅하고 무시 (크롤링 실패로 이어지지 않도록)
                        log.error("Discord notification failed but continuing: {}", error.getMessage());
                        return Mono.empty();
                    })
                    .block(); // 동기 호출 (이벤트 리스너가 비동기로 처리하므로)

            log.info("Discord webhook sent successfully");

        } catch (Exception e) {
            log.error("Unexpected error sending Discord webhook: {}", e.getMessage(), e);
        }
    }

    /**
     * 재시도 가능한 예외인지 확인
     */
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            int status = ex.getStatusCode().value();
            // 429 (Too Many Requests) 또는 5xx 서버 에러만 재시도
            return status == 429 || (status >= 500 && status < 600);
        }
        // 네트워크 에러 등은 재시도
        return !(throwable instanceof IllegalArgumentException);
    }
}
