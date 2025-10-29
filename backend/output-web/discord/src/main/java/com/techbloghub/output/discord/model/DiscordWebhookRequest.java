package com.techbloghub.output.discord.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Discord Webhook 요청 DTO
 * Discord Webhook API 스펙: https://discord.com/developers/docs/resources/webhook#execute-webhook
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordWebhookRequest {

    /**
     * 메시지 내용 (텍스트)
     */
    private String content;

    /**
     * 사용자명 (봇 이름)
     */
    private String username;

    /**
     * Embed 목록
     */
    private List<DiscordEmbed> embeds;
}
