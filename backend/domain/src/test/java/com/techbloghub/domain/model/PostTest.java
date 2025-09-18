package com.techbloghub.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class PostTest {

    @Test
    @DisplayName("필수 필드가 모두 있는 포스트는 유효하다")
    void 유효한_포스트_검증_성공() {
        // given
        Blog blog = createValidBlog();
        Post post = Post.builder()
                .title("테스트 포스트")
                .originalUrl("https://example.com/post/1")
                .normalizedUrl("https://example.com/post/1")
                .blog(blog)
                .build();

        // when
        boolean isValid = post.isValid();

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("제목이 null인 포스트는 유효하지 않다")
    void 제목_null_포스트_검증_실패() {
        // given
        Blog blog = createValidBlog();
        Post post = Post.builder()
                .title(null)
                .originalUrl("https://example.com/post/1")
                .normalizedUrl("https://example.com/post/1")
                .blog(blog)
                .build();

        // when
        boolean isValid = post.isValid();

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("제목이 빈 문자열인 포스트는 유효하지 않다")
    void 제목_빈문자열_포스트_검증_실패() {
        // given
        Blog blog = createValidBlog();
        Post post = Post.builder()
                .title("   ")
                .originalUrl("https://example.com/post/1")
                .normalizedUrl("https://example.com/post/1")
                .blog(blog)
                .build();

        // when
        boolean isValid = post.isValid();

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("원본 URL이 null인 포스트는 유효하지 않다")
    void 원본URL_null_포스트_검증_실패() {
        // given
        Blog blog = createValidBlog();
        Post post = Post.builder()
                .title("테스트 포스트")
                .originalUrl(null)
                .normalizedUrl("https://example.com/post/1")
                .blog(blog)
                .build();

        // when
        boolean isValid = post.isValid();

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("정규화 URL이 null인 포스트는 유효하지 않다")
    void 정규화URL_null_포스트_검증_실패() {
        // given
        Blog blog = createValidBlog();
        Post post = Post.builder()
                .title("테스트 포스트")
                .originalUrl("https://example.com/post/1")
                .normalizedUrl(null)
                .blog(blog)
                .build();

        // when
        boolean isValid = post.isValid();

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("블로그가 null인 포스트는 유효하지 않다")
    void 블로그_null_포스트_검증_실패() {
        // given
        Post post = Post.builder()
                .title("테스트 포스트")
                .originalUrl("https://example.com/post/1")
                .normalizedUrl("https://example.com/post/1")
                .blog(null)
                .build();

        // when
        boolean isValid = post.isValid();

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("태그가 있는 포스트에서 태그명 목록을 반환한다")
    void 태그명_목록_반환_성공() {
        // given
        Tag tag1 = Tag.builder().name("Java").build();
        Tag tag2 = Tag.builder().name("Spring").build();
        Set<Tag> tags = Set.of(tag1, tag2);

        Post post = Post.builder()
                .title("테스트 포스트")
                .tags(tags)
                .build();

        // when
        Set<String> tagNames = post.getTagNames();

        // then
        assertThat(tagNames).containsExactlyInAnyOrder("Java", "Spring");
    }

    @Test
    @DisplayName("태그가 없는 포스트에서 빈 Set을 반환한다")
    void 태그_없는_포스트_빈Set_반환() {
        // given
        Post post = Post.builder()
                .title("테스트 포스트")
                .tags(null)
                .build();

        // when
        Set<String> tagNames = post.getTagNames();

        // then
        assertThat(tagNames).isEmpty();
    }

    @Test
    @DisplayName("카테고리가 있는 포스트에서 카테고리명 목록을 반환한다")
    void 카테고리명_목록_반환_성공() {
        // given
        Category category1 = Category.builder().name("백엔드").build();
        Category category2 = Category.builder().name("프론트엔드").build();
        Set<Category> categories = Set.of(category1, category2);

        Post post = Post.builder()
                .title("테스트 포스트")
                .categories(categories)
                .build();

        // when
        Set<String> categoryNames = post.getCategoryNames();

        // then
        assertThat(categoryNames).containsExactlyInAnyOrder("백엔드", "프론트엔드");
    }

    @Test
    @DisplayName("카테고리가 없는 포스트에서 빈 Set을 반환한다")
    void 카테고리_없는_포스트_빈Set_반환() {
        // given
        Post post = Post.builder()
                .title("테스트 포스트")
                .categories(null)
                .build();

        // when
        Set<String> categoryNames = post.getCategoryNames();

        // then
        assertThat(categoryNames).isEmpty();
    }

    @Test
    @DisplayName("쿼리 파라미터가 있는 URL을 정규화한다")
    void URL_쿼리파라미터_제거_정규화() {
        // given
        String urlWithQuery = "https://example.com/post/1?utm_source=google&utm_medium=cpc";

        // when
        String normalized = Post.normalizeUrl(urlWithQuery);

        // then
        assertThat(normalized).isEqualTo("https://example.com/post/1");
    }

    @Test
    @DisplayName("프래그먼트가 있는 URL을 정규화한다")
    void URL_프래그먼트_제거_정규화() {
        // given
        String urlWithFragment = "https://example.com/post/1#section1";

        // when
        String normalized = Post.normalizeUrl(urlWithFragment);

        // then
        assertThat(normalized).isEqualTo("https://example.com/post/1");
    }

    @Test
    @DisplayName("마지막 슬래시가 있는 URL을 정규화한다")
    void URL_마지막슬래시_제거_정규화() {
        // given
        String urlWithTrailingSlash = "https://example.com/post/1/";

        // when
        String normalized = Post.normalizeUrl(urlWithTrailingSlash);

        // then
        assertThat(normalized).isEqualTo("https://example.com/post/1");
    }

    @Test
    @DisplayName("대문자가 포함된 URL을 소문자로 정규화한다")
    void URL_대문자_소문자_정규화() {
        // given
        String upperCaseUrl = "https://Example.COM/Post/1";

        // when
        String normalized = Post.normalizeUrl(upperCaseUrl);

        // then
        assertThat(normalized).isEqualTo("https://example.com/post/1");
    }

    @Test
    @DisplayName("복합적인 경우의 URL을 정규화한다")
    void URL_복합_정규화() {
        // given
        String complexUrl = "https://Example.COM/Post/1/?utm_source=google#section1";

        // when
        String normalized = Post.normalizeUrl(complexUrl);

        // then
        assertThat(normalized).isEqualTo("https://example.com/post/1");
    }

    @Test
    @DisplayName("null URL은 빈 문자열로 정규화한다")
    void URL_null_빈문자열_정규화() {
        // when
        String normalized = Post.normalizeUrl(null);

        // then
        assertThat(normalized).isEqualTo("");
    }

    @Test
    @DisplayName("빈 문자열 URL은 빈 문자열로 정규화한다")
    void URL_빈문자열_정규화() {
        // when
        String normalized = Post.normalizeUrl("   ");

        // then
        assertThat(normalized).isEqualTo("");
    }

    private Blog createValidBlog() {
        return Blog.builder()
                .id(1L)
                .name("테스트 블로그")
                .company("테스트 회사")
                .rssUrl("https://example.com/rss")
                .siteUrl("https://example.com")
                .status(BlogStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }
}