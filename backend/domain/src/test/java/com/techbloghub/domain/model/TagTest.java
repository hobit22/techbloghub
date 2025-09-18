package com.techbloghub.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class TagTest {

    @Test
    @DisplayName("동일한 id와 name을 가진 태그는 같다고 판단한다")
    void 동일한_id와_name_태그_동등성_확인() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Tag tag1 = Tag.builder()
                .id(1L)
                .name("Java")
                .description("Java 프로그래밍 언어")
                .tagGroup("language")
                .createdAt(now)
                .build();

        Tag tag2 = Tag.builder()
                .id(1L)
                .name("Java")
                .description("다른 설명")
                .tagGroup("다른 그룹")
                .createdAt(now.plusDays(1))
                .build();

        // when & then
        assertThat(tag1).isEqualTo(tag2);
        assertThat(tag1.hashCode()).isEqualTo(tag2.hashCode());
    }

    @Test
    @DisplayName("다른 id를 가진 태그는 다르다고 판단한다")
    void 다른_id_태그_동등성_실패() {
        // given
        Tag tag1 = Tag.builder()
                .id(1L)
                .name("Java")
                .build();

        Tag tag2 = Tag.builder()
                .id(2L)
                .name("Java")
                .build();

        // when & then
        assertThat(tag1).isNotEqualTo(tag2);
    }

    @Test
    @DisplayName("다른 name을 가진 태그는 다르다고 판단한다")
    void 다른_name_태그_동등성_실패() {
        // given
        Tag tag1 = Tag.builder()
                .id(1L)
                .name("Java")
                .build();

        Tag tag2 = Tag.builder()
                .id(1L)
                .name("Python")
                .build();

        // when & then
        assertThat(tag1).isNotEqualTo(tag2);
    }
}