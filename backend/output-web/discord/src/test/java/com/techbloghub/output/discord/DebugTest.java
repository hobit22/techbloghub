package com.techbloghub.output.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techbloghub.output.discord.model.DiscordEmbed;
import com.techbloghub.output.discord.model.DiscordWebhookRequest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * JSON 직렬화 디버깅 테스트
 */
class DebugTest {

    @Test
    void printEmbedJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

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

        String json = mapper.writeValueAsString(request);
        System.out.println("=== Discord Webhook Request JSON ===");
        System.out.println(json);
    }
}
