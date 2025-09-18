package com.techbloghub.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class BlogTest {

    @Test
    @DisplayName("ACTIVE 상태의 블로그는 활성화된 상태이다")
    void ACTIVE_상태_블로그_활성화_확인() {
        // given
        Blog blog = Blog.builder()
                .status(BlogStatus.ACTIVE)
                .build();

        // when
        boolean isActive = blog.isActive();

        // then
        assertThat(isActive).isTrue();
    }

    @Test
    @DisplayName("INACTIVE 상태의 블로그는 비활성화된 상태이다")
    void INACTIVE_상태_블로그_비활성화_확인() {
        // given
        Blog blog = Blog.builder()
                .status(BlogStatus.INACTIVE)
                .build();

        // when
        boolean isActive = blog.isActive();

        // then
        assertThat(isActive).isFalse();
    }

    @Test
    @DisplayName("마지막 크롤링이 null인 경우 크롤링이 필요하다")
    void 마지막크롤링_null_크롤링필요() {
        // given
        Blog blog = Blog.builder()
                .lastCrawledAt(null)
                .build();

        // when
        boolean needsCrawling = blog.needsCrawling();

        // then
        assertThat(needsCrawling).isTrue();
    }

    @Test
    @DisplayName("마지막 크롤링이 1시간 이전인 경우 크롤링이 필요하다")
    void 마지막크롤링_1시간이전_크롤링필요() {
        // given
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        Blog blog = Blog.builder()
                .lastCrawledAt(twoHoursAgo)
                .build();

        // when
        boolean needsCrawling = blog.needsCrawling();

        // then
        assertThat(needsCrawling).isTrue();
    }

    @Test
    @DisplayName("마지막 크롤링이 30분 이전인 경우 크롤링이 필요하지 않다")
    void 마지막크롤링_30분이전_크롤링불필요() {
        // given
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        Blog blog = Blog.builder()
                .lastCrawledAt(thirtyMinutesAgo)
                .build();

        // when
        boolean needsCrawling = blog.needsCrawling();

        // then
        assertThat(needsCrawling).isFalse();
    }

    @Test
    @DisplayName("59분 전 크롤링인 경우 크롤링이 필요하지 않다")
    void 마지막크롤링_59분전_크롤링불필요() {
        // given
        LocalDateTime fiftyNineMinutesAgo = LocalDateTime.now().minusMinutes(59);
        Blog blog = Blog.builder()
                .lastCrawledAt(fiftyNineMinutesAgo)
                .build();

        // when
        boolean needsCrawling = blog.needsCrawling();

        // then
        assertThat(needsCrawling).isFalse();
    }

    @Test
    @DisplayName("유효한 HTTP RSS URL은 검증을 통과한다")
    void 유효한_HTTP_RSS_URL_검증_성공() {
        // given
        Blog blog = Blog.builder()
                .rssUrl("http://example.com/rss")
                .build();

        // when
        boolean hasValidRssUrl = blog.hasValidRssUrl();

        // then
        assertThat(hasValidRssUrl).isTrue();
    }

    @Test
    @DisplayName("유효한 HTTPS RSS URL은 검증을 통과한다")
    void 유효한_HTTPS_RSS_URL_검증_성공() {
        // given
        Blog blog = Blog.builder()
                .rssUrl("https://example.com/rss")
                .build();

        // when
        boolean hasValidRssUrl = blog.hasValidRssUrl();

        // then
        assertThat(hasValidRssUrl).isTrue();
    }

    @Test
    @DisplayName("null RSS URL은 검증을 실패한다")
    void null_RSS_URL_검증_실패() {
        // given
        Blog blog = Blog.builder()
                .rssUrl(null)
                .build();

        // when
        boolean hasValidRssUrl = blog.hasValidRssUrl();

        // then
        assertThat(hasValidRssUrl).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 RSS URL은 검증을 실패한다")
    void 빈문자열_RSS_URL_검증_실패() {
        // given
        Blog blog = Blog.builder()
                .rssUrl("   ")
                .build();

        // when
        boolean hasValidRssUrl = blog.hasValidRssUrl();

        // then
        assertThat(hasValidRssUrl).isFalse();
    }

    @Test
    @DisplayName("HTTP/HTTPS로 시작하지 않는 RSS URL은 검증을 실패한다")
    void 잘못된프로토콜_RSS_URL_검증_실패() {
        // given
        Blog blog = Blog.builder()
                .rssUrl("ftp://example.com/rss")
                .build();

        // when
        boolean hasValidRssUrl = blog.hasValidRssUrl();

        // then
        assertThat(hasValidRssUrl).isFalse();
    }

    @Test
    @DisplayName("유효한 정보로 블로그를 생성할 수 있다")
    void 유효한_정보로_블로그_생성_성공() {
        // given
        String name = "테스트 블로그";
        String company = "테스트 회사";
        String rssUrl = "https://example.com/rss";
        String siteUrl = "https://example.com";
        String logoUrl = "https://example.com/logo.png";
        String description = "테스트 블로그 설명";

        // when
        Blog blog = Blog.of(name, company, rssUrl, siteUrl, logoUrl, description);

        // then
        assertThat(blog.getName()).isEqualTo(name);
        assertThat(blog.getCompany()).isEqualTo(company);
        assertThat(blog.getRssUrl()).isEqualTo(rssUrl);
        assertThat(blog.getSiteUrl()).isEqualTo(siteUrl);
        assertThat(blog.getLogoUrl()).isEqualTo(logoUrl);
        assertThat(blog.getDescription()).isEqualTo(description);
        assertThat(blog.getStatus()).isEqualTo(BlogStatus.ACTIVE);
    }

    @Test
    @DisplayName("공백이 포함된 정보로 블로그 생성시 공백이 제거된다")
    void 공백포함_정보로_블로그_생성시_공백제거() {
        // given
        String name = "  테스트 블로그  ";
        String company = "  테스트 회사  ";
        String rssUrl = "  https://example.com/rss  ";
        String siteUrl = "  https://example.com  ";
        String logoUrl = "  https://example.com/logo.png  ";
        String description = "  테스트 블로그 설명  ";

        // when
        Blog blog = Blog.of(name, company, rssUrl, siteUrl, logoUrl, description);

        // then
        assertThat(blog.getName()).isEqualTo("테스트 블로그");
        assertThat(blog.getCompany()).isEqualTo("테스트 회사");
        assertThat(blog.getRssUrl()).isEqualTo("https://example.com/rss");
        assertThat(blog.getSiteUrl()).isEqualTo("https://example.com");
        assertThat(blog.getLogoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(blog.getDescription()).isEqualTo("테스트 블로그 설명");
    }

    @Test
    @DisplayName("빈 로고 URL로 블로그 생성시 null로 설정된다")
    void 빈_로고URL로_블로그_생성시_null설정() {
        // given
        String logoUrl = "   ";

        // when
        Blog blog = Blog.of("테스트 블로그", "테스트 회사", "https://example.com/rss",
                           "https://example.com", logoUrl, "설명");

        // then
        assertThat(blog.getLogoUrl()).isNull();
    }

    @Test
    @DisplayName("null 로고 URL로 블로그 생성시 null로 설정된다")
    void null_로고URL로_블로그_생성시_null설정() {
        // when
        Blog blog = Blog.of("테스트 블로그", "테스트 회사", "https://example.com/rss",
                           "https://example.com", null, "설명");

        // then
        assertThat(blog.getLogoUrl()).isNull();
    }

    @Test
    @DisplayName("null 설명으로 블로그 생성시 null로 설정된다")
    void null_설명으로_블로그_생성시_null설정() {
        // when
        Blog blog = Blog.of("테스트 블로그", "테스트 회사", "https://example.com/rss",
                           "https://example.com", "https://example.com/logo.png", null);

        // then
        assertThat(blog.getDescription()).isNull();
    }

    @Test
    @DisplayName("기본 상태는 ACTIVE로 설정된다")
    void 기본상태_ACTIVE_설정() {
        // when
        Blog blog = Blog.of("테스트 블로그", "테스트 회사", "https://example.com/rss",
                           "https://example.com", "https://example.com/logo.png", "설명");

        // then
        assertThat(blog.getStatus()).isEqualTo(BlogStatus.ACTIVE);
        assertThat(blog.isActive()).isTrue();
    }
}