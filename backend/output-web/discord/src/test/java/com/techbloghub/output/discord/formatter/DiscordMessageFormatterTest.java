package com.techbloghub.output.discord.formatter;

import com.techbloghub.domain.crawling.model.CrawlingResult;
import com.techbloghub.output.discord.client.DiscordWebhookClient;
import com.techbloghub.output.discord.config.DiscordConfig;
import com.techbloghub.output.discord.model.DiscordWebhookRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 실제 크롤링 결과를 Discord 메시지로 변환하는 테스트
 */
class DiscordMessageFormatterTest {

    public static final String URL = "https://discord.com/api/webhooks/1433007314402803722/tt4Az6fngMgxObLZ2R_yoG274geK5OTn5BghwLhpLFJtO-kh46wEWLgBnacrCqJ0r-ea";

    @Test
    void testSuccessfulCrawling() {
        // Given: Discord 설정 (Environment mock 필요)
        DiscordConfig config = createDiscordConfig();
        DiscordMessageFormatter formatter = new DiscordMessageFormatter(config);

        // Given: 성공적인 크롤링 결과
        CrawlingResult result = CrawlingResult.builder()
                .startTime(LocalDateTime.now().minusMinutes(2))
                .endTime(LocalDateTime.now())
                .totalBlogs(5)
                .processedBlogs(5)
                .totalPostsSaved(10)
                .blogResults(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();

        // When: 메시지 포맷팅
        DiscordWebhookRequest request = formatter.format(result);

        // Then: Discord 전송
        DiscordWebhookClient client = new DiscordWebhookClient(config);
        client.sendWebhook(request);
    }

    private DiscordConfig createDiscordConfig() {
        // Mock Environment for testing
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setActiveProfiles("local");

        DiscordConfig config = new DiscordConfig(mockEnv);
        config.setUrl(URL);
        config.setEnabled(true);
        return config;
    }

    @Test
    void testPartialSuccessCrawling() {
        // Given: Discord 설정
        DiscordConfig config = createDiscordConfig();
        DiscordMessageFormatter formatter = new DiscordMessageFormatter(config);

        // Given: 부분 성공 크롤링 결과 (일부 에러 포함)
        List<CrawlingResult.CrawlingError> errors = List.of(
                CrawlingResult.CrawlingError.builder()
                        .blogId(1L)
                        .blogName("실패한 블로그 1")
                        .errorMessage("Connection timeout after 30 seconds")
                        .errorType("TimeoutException")
                        .occurredAt(LocalDateTime.now())
                        .build(),
                CrawlingResult.CrawlingError.builder()
                        .blogId(2L)
                        .blogName("실패한 블로그 2")
                        .errorMessage("Invalid RSS feed format")
                        .errorType("RssParseException")
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        CrawlingResult result = CrawlingResult.builder()
                .startTime(LocalDateTime.now().minusMinutes(3))
                .endTime(LocalDateTime.now())
                .totalBlogs(7)
                .processedBlogs(5)
                .totalPostsSaved(8)
                .blogResults(new ArrayList<>())
                .errors(errors)
                .build();

        // When & Then
        DiscordWebhookRequest request = formatter.format(result);

        DiscordWebhookClient client = new DiscordWebhookClient(config);
        client.sendWebhook(request);
    }

    @Test
    void testFailedCrawling() {
        // Given: Discord 설정
        DiscordConfig config = createDiscordConfig();
        DiscordMessageFormatter formatter = new DiscordMessageFormatter(config);

        // Given: 완전 실패 크롤링 결과
        List<CrawlingResult.CrawlingError> errors = List.of(
                CrawlingResult.CrawlingError.builder()
                        .blogId(1L)
                        .blogName("블로그 A")
                        .errorMessage("Network unreachable")
                        .errorType("NetworkException")
                        .occurredAt(LocalDateTime.now())
                        .build(),
                CrawlingResult.CrawlingError.builder()
                        .blogId(2L)
                        .blogName("블로그 B")
                        .errorMessage("DNS resolution failed")
                        .errorType("DNSException")
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        CrawlingResult result = CrawlingResult.builder()
                .startTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now())
                .totalBlogs(2)
                .processedBlogs(0)
                .totalPostsSaved(0)
                .blogResults(new ArrayList<>())
                .errors(errors)
                .build();

        // When & Then
        DiscordWebhookRequest request = formatter.format(result);

        DiscordWebhookClient client = new DiscordWebhookClient(config);
        client.sendWebhook(request);
    }

    @Test
    void testProductionEnvironment() {
        // Given: Production 환경 설정
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setActiveProfiles("prod");

        DiscordConfig config = new DiscordConfig(mockEnv);
        config.setUrl(URL);
        config.setEnabled(true);

        DiscordMessageFormatter formatter = new DiscordMessageFormatter(config);

        // Given: 성공 결과
        CrawlingResult result = CrawlingResult.builder()
                .startTime(LocalDateTime.now().minusMinutes(2))
                .endTime(LocalDateTime.now())
                .totalBlogs(10)
                .processedBlogs(10)
                .totalPostsSaved(25)
                .blogResults(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();

        // When & Then
        DiscordWebhookRequest request = formatter.format(result);

        DiscordWebhookClient client = new DiscordWebhookClient(config);
        client.sendWebhook(request);
    }
}
