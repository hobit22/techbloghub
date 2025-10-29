package com.techbloghub.output.discord.formatter;

import com.techbloghub.domain.crawling.model.CrawlingResult;
import com.techbloghub.output.discord.config.DiscordConfig;
import com.techbloghub.output.discord.model.DiscordEmbed;
import com.techbloghub.output.discord.model.DiscordWebhookRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Discord 메시지 포맷터
 * CrawlingResult를 Discord Webhook 요청으로 변환
 */
@Component
@RequiredArgsConstructor
public class DiscordMessageFormatter {

    private static final String BOT_USERNAME = "TechBlogHub Crawler";
    private static final int MAX_ERROR_DISPLAY = 5;

    private final DiscordConfig discordConfig;

    /**
     * 크롤링 결과를 Discord Webhook 요청으로 변환
     */
    public DiscordWebhookRequest format(CrawlingResult result) {
        String footerText = String.format("TechBlogHub RSS Crawler • Environment: %s",
                discordConfig.getActiveProfile());

        DiscordEmbed embed = DiscordEmbed.builder()
                .title(buildTitle(result))
                .color(determineColor(result))
                .fields(buildFields(result))
                .timestamp(result.getEndTime().atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .footer(DiscordEmbed.Footer.builder()
                        .text(footerText)
                        .build())
                .build();

        return DiscordWebhookRequest.builder()
                .username(BOT_USERNAME)
                .embeds(List.of(embed))
                .build();
    }

    /**
     * 제목 생성
     */
    private String buildTitle(CrawlingResult result) {
        String profileName = discordConfig.getActiveProfile();

        String status;
        if (result.getTotalPostsSaved() > 0 && result.getErrors().isEmpty()) {
            status = "✅ RSS 크롤링 완료";
        } else if (result.getTotalPostsSaved() > 0) {
            status = "⚠️ RSS 크롤링 부분 성공";
        } else if (!result.getErrors().isEmpty()) {
            status = "❌ RSS 크롤링 실패";
        } else {
            status = "ℹ️ RSS 크롤링 완료 (새 글 없음)";
        }

        return String.format("[%s] %s", profileName, status);
    }

    /**
     * 색상 결정
     */
    private int determineColor(CrawlingResult result) {
        boolean hasNewPosts = result.getTotalPostsSaved() > 0;
        boolean hasErrors = !result.getErrors().isEmpty();

        if (hasNewPosts && !hasErrors) {
            return DiscordEmbed.Color.GREEN;    // 성공
        } else if (hasNewPosts) {
            return DiscordEmbed.Color.YELLOW;   // 부분 성공
        } else if (hasErrors) {
            return DiscordEmbed.Color.RED;      // 실패
        } else {
            return DiscordEmbed.Color.BLUE;     // 정보
        }
    }

    /**
     * 필드 목록 생성
     */
    private List<DiscordEmbed.Field> buildFields(CrawlingResult result) {
        List<DiscordEmbed.Field> fields = new ArrayList<>();

        // 요약 정보
        String summary = String.format(
                "**총 블로그**: %d개\n**처리된 블로그**: %d개\n**새 글**: %d개",
                result.getTotalBlogs(),
                result.getProcessedBlogs(),
                result.getTotalPostsSaved()
        );
        fields.add(DiscordEmbed.Field.builder()
                .name("📊 요약")
                .value(summary)
                .inline(false)
                .build());

        // 실행 시간
        fields.add(DiscordEmbed.Field.builder()
                .name("⏱️ 실행 시간")
                .value(result.getExecutionTimeInSeconds() + "초")
                .inline(true)
                .build());

        // 에러 정보 (있는 경우만)
        if (!result.getErrors().isEmpty()) {
            String errorInfo = buildErrorInfo(result.getErrors());
            fields.add(DiscordEmbed.Field.builder()
                    .name("❌ 에러 (" + result.getErrors().size() + "건)")
                    .value(errorInfo)
                    .inline(false)
                    .build());
        }

        return fields;
    }

    /**
     * 에러 정보 생성
     */
    private String buildErrorInfo(List<CrawlingResult.CrawlingError> errors) {
        List<CrawlingResult.CrawlingError> displayErrors = errors.stream()
                .limit(MAX_ERROR_DISPLAY)
                .toList();

        StringBuilder sb = new StringBuilder();
        for (CrawlingResult.CrawlingError error : displayErrors) {
            sb.append(String.format("• **%s**: %s\n",
                    error.getBlogName(),
                    truncateErrorMessage(error.getErrorMessage())));
        }

        if (errors.size() > MAX_ERROR_DISPLAY) {
            sb.append(String.format("\n... 외 %d건", errors.size() - MAX_ERROR_DISPLAY));
        }

        return sb.toString();
    }

    /**
     * 에러 메시지 자르기 (너무 길면 축약)
     */
    private String truncateErrorMessage(String message) {
        int maxLength = 100;
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }
}
