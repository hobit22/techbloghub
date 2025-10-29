package com.techbloghub.output.discord.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Discord Embed 객체
 * Discord Webhook API 스펙: https://discord.com/developers/docs/resources/webhook#execute-webhook
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordEmbed {

    /**
     * Embed 제목
     */
    private String title;

    /**
     * Embed 설명
     */
    private String description;

    /**
     * 색상 (10진수)
     */
    private Integer color;

    /**
     * 필드 목록
     */
    private List<Field> fields;

    /**
     * 타임스탬프 (ISO8601)
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;

    /**
     * Footer
     */
    private Footer footer;

    /**
     * Embed 필드
     */
    @Getter
    @Builder
    public static class Field {
        private String name;
        private String value;
        @JsonProperty("inline")
        private boolean inline;
    }

    /**
     * Embed Footer
     */
    @Getter
    @Builder
    public static class Footer {
        private String text;
    }

    /**
     * 색상 헬퍼 메서드
     */
    public static class Color {
        public static final int GREEN = 0x00ff00;    // 성공
        public static final int YELLOW = 0xffff00;   // 부분 성공
        public static final int RED = 0xff0000;      // 실패
        public static final int BLUE = 0x0099ff;     // 정보
    }
}
