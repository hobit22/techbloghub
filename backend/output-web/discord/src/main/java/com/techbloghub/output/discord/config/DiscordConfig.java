package com.techbloghub.output.discord.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Discord Webhook 설정
 */
@Configuration
@ConfigurationProperties(prefix = "discord.webhook")
@Getter
@Setter
@RequiredArgsConstructor
public class DiscordConfig {

    private final Environment environment;

    /**
     * Discord Webhook URL
     */
    private String url;

    /**
     * Discord 알림 활성화 여부
     */
    private boolean enabled = false;

    public boolean isConfigured() {
        return enabled && url != null && !url.isBlank();
    }

    /**
     * 현재 활성 프로파일 가져오기
     */
    public String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length > 0) {
            return profiles[0];
        }
        return "default";
    }
}
