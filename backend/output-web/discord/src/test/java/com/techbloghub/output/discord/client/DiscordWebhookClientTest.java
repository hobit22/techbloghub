package com.techbloghub.output.discord.client;

import com.techbloghub.output.discord.config.DiscordConfig;
import com.techbloghub.output.discord.model.DiscordEmbed;
import com.techbloghub.output.discord.model.DiscordWebhookRequest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DiscordWebhookClient 간단한 테스트
 */
class DiscordWebhookClientTest {

    public static final String URL = "https://discord.com/api/webhooks/1433007314402803722/tt4Az6fngMgxObLZ2R_yoG274geK5OTn5BghwLhpLFJtO-kh46wEWLgBnacrCqJ0r-ea";

    @Test
    void sendTestWebhook() {
        // Given: Discord 설정
        org.springframework.mock.env.MockEnvironment mockEnv = new org.springframework.mock.env.MockEnvironment();
        mockEnv.setActiveProfiles("local");

        DiscordConfig config = new DiscordConfig(mockEnv);
        config.setUrl(URL);
        config.setEnabled(true);

        DiscordWebhookClient client = new DiscordWebhookClient(config);

        // When: 간단한 텍스트 메시지만 전송
        DiscordWebhookRequest request = DiscordWebhookRequest.builder()
                .content("🧪 **테스트 메시지** - Discord 알림 기능이 정상 작동합니다!")
                .username("TechBlogHub Test")
                .build();

        // Then: 전송
        client.sendWebhook(request);
    }

    @Test
    void sendTestWebhookWithEmbed() {
        // Given: Discord 설정
        org.springframework.mock.env.MockEnvironment mockEnv = new org.springframework.mock.env.MockEnvironment();
        mockEnv.setActiveProfiles("local");

        DiscordConfig config = new DiscordConfig(mockEnv);
        config.setUrl(URL);
        config.setEnabled(true);

        DiscordWebhookClient client = new DiscordWebhookClient(config);

        // When: Embed 포함 메시지
        DiscordWebhookRequest request = DiscordWebhookRequest.builder()
                .username("TechBlogHub Test")
                .embeds(List.of(
                        DiscordEmbed.builder()
                                .title("🧪 테스트 메시지")
                                .description("Discord 알림 기능 테스트입니다.")
                                .color(DiscordEmbed.Color.BLUE)
                                .fields(List.of(
                                        DiscordEmbed.Field.builder()
                                                .name("테스트 항목")
                                                .value("Webhook 전송 성공!")
                                                .inline(false)
                                                .build()
                                ))
                                .timestamp(OffsetDateTime.now())
                                .footer(DiscordEmbed.Footer.builder()
                                        .text("Test Footer")
                                        .build())
                                .build()
                ))
                .build();

        // Then: 전송
        client.sendWebhook(request);
    }
}
