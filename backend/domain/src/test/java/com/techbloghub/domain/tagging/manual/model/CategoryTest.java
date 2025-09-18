package com.techbloghub.domain.tagging.manual.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    @DisplayName("유효한 카테고리명을 가진 카테고리는 검증을 통과한다")
    void 유효한_카테고리명_검증_성공() {
        // given
        Category category = Category.builder()
                .name("백엔드")
                .build();

        // when
        boolean isValid = category.isValid();

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("null 카테고리명을 가진 카테고리는 검증을 실패한다")
    void null_카테고리명_검증_실패() {
        // given
        Category category = Category.builder()
                .name(null)
                .build();

        // when
        boolean isValid = category.isValid();

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 카테고리명을 가진 카테고리는 검증을 실패한다")
    void 빈문자열_카테고리명_검증_실패() {
        // given
        Category category = Category.builder()
                .name("   ")
                .build();

        // when
        boolean isValid = category.isValid();

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("100자를 초과하는 카테고리명을 가진 카테고리는 검증을 실패한다")
    void 긴_카테고리명_검증_실패() {
        // given
        String longName = "a".repeat(101); // 101자
        Category category = Category.builder()
                .name(longName)
                .build();

        // when
        boolean isValid = category.isValid();

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("정확히 100자인 카테고리명을 가진 카테고리는 검증을 통과한다")
    void 최대길이_카테고리명_검증_성공() {
        // given
        String maxLengthName = "a".repeat(100); // 100자
        Category category = Category.builder()
                .name(maxLengthName)
                .build();

        // when
        boolean isValid = category.isValid();

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("카테고리명을 트림하여 정규화한다")
    void 카테고리명_트림_정규화() {
        // given
        Category category = Category.builder()
                .name("  백엔드 개발  ")
                .build();

        // when
        String normalizedName = category.getNormalizedName();

        // then
        assertThat(normalizedName).isEqualTo("백엔드 개발");
    }

    @Test
    @DisplayName("null 카테고리명은 null로 정규화한다")
    void null_카테고리명_정규화() {
        // given
        Category category = Category.builder()
                .name(null)
                .build();

        // when
        String normalizedName = category.getNormalizedName();

        // then
        assertThat(normalizedName).isNull();
    }

    @Test
    @DisplayName("동일한 id와 name을 가진 카테고리는 같다고 판단한다")
    void 동일한_id와_name_카테고리_동등성_확인() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Category category1 = Category.builder()
                .id(1L)
                .name("백엔드")
                .description("백엔드 개발")
                .createdAt(now)
                .build();

        Category category2 = Category.builder()
                .id(1L)
                .name("백엔드")
                .description("다른 설명")
                .createdAt(now.plusDays(1))
                .build();

        // when & then
        assertThat(category1).isEqualTo(category2);
        assertThat(category1.hashCode()).isEqualTo(category2.hashCode());
    }

    @Test
    @DisplayName("다른 id를 가진 카테고리는 다르다고 판단한다")
    void 다른_id_카테고리_동등성_실패() {
        // given
        Category category1 = Category.builder()
                .id(1L)
                .name("백엔드")
                .build();

        Category category2 = Category.builder()
                .id(2L)
                .name("백엔드")
                .build();

        // when & then
        assertThat(category1).isNotEqualTo(category2);
    }

    @Test
    @DisplayName("다른 name을 가진 카테고리는 다르다고 판단한다")
    void 다른_name_카테고리_동등성_실패() {
        // given
        Category category1 = Category.builder()
                .id(1L)
                .name("백엔드")
                .build();

        Category category2 = Category.builder()
                .id(1L)
                .name("프론트엔드")
                .build();

        // when & then
        assertThat(category1).isNotEqualTo(category2);
    }
}