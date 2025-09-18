package com.techbloghub.domain.tagging.auto.service;

import com.techbloghub.domain.post.port.PostRepositoryPort;
import com.techbloghub.domain.post.model.Post;
import com.techbloghub.domain.tagging.auto.model.TaggingProcessStatus;
import com.techbloghub.domain.tagging.auto.model.TaggingResult;
import com.techbloghub.domain.tagging.auto.port.RejectedItemPort;
import com.techbloghub.domain.tagging.auto.port.TagPersistencePort;
import com.techbloghub.domain.tagging.auto.port.TaggingProcessorPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoTaggingServiceTest {

    @Mock
    private PostRepositoryPort postRepositoryPort;

    @Mock
    private TaggingProcessorPort taggingProcessor;

    @Mock
    private TagPersistencePort tagPersistenceService;

    @Mock
    private RejectedItemPort rejectedItemService;

    @InjectMocks
    private AutoTaggingService autoTaggingService;

    @Test
    @DisplayName("포스트에 태그와 카테고리를 모두 성공적으로 할당한다")
    void 포스트_태그_카테고리_자동할당_성공() {
        // given
        Long postId = 1L;
        Post post = createTestPost(postId);
        given(postRepositoryPort.findById(postId)).willReturn(Optional.of(post));

        TaggingResult taggingResult = new TaggingResult(
                List.of("Java", "Spring"),
                List.of("백엔드"),
                List.of("Python"),
                List.of("데이터사이언스")
        );
        given(taggingProcessor.processTagging(post)).willReturn(taggingResult);
        given(tagPersistenceService.persistTaggingResult(post, taggingResult))
                .willReturn(TaggingProcessStatus.TAGGED_AND_CATEGORIZED);

        // when
        TaggingResult result = autoTaggingService.autoTag(postId);

        // then
        assertThat(result.tags()).containsExactly("Java", "Spring");
        assertThat(result.categories()).containsExactly("백엔드");
        assertThat(result.rejectedTags()).containsExactly("Python");
        assertThat(result.rejectedCategories()).containsExactly("데이터사이언스");

        verify(taggingProcessor).processTagging(post);
        verify(tagPersistenceService).persistTaggingResult(post, taggingResult);
        verify(postRepositoryPort).updateTaggingStatus(postId, TaggingProcessStatus.TAGGED_AND_CATEGORIZED);
        verify(rejectedItemService).saveRejectedItems(post, taggingResult);
    }

    @Test
    @DisplayName("태그만 할당되고 카테고리가 없는 경우 TAGGED 상태로 설정한다")
    void 태그만_할당_TAGGED_상태_설정() {
        // given
        Long postId = 1L;
        Post post = createTestPost(postId);
        given(postRepositoryPort.findById(postId)).willReturn(Optional.of(post));

        TaggingResult taggingResult = new TaggingResult(
                List.of("Java"),
                List.of(),
                List.of(),
                List.of()
        );
        given(taggingProcessor.processTagging(post)).willReturn(taggingResult);
        given(tagPersistenceService.persistTaggingResult(post, taggingResult))
                .willReturn(TaggingProcessStatus.TAGGED);

        // when
        TaggingResult result = autoTaggingService.autoTag(postId);

        // then
        assertThat(result.tags()).containsExactly("Java");
        assertThat(result.categories()).isEmpty();

        verify(taggingProcessor).processTagging(post);
        verify(tagPersistenceService).persistTaggingResult(post, taggingResult);
        verify(postRepositoryPort).updateTaggingStatus(postId, TaggingProcessStatus.TAGGED);
        verify(rejectedItemService).saveRejectedItems(post, taggingResult);
    }

    @Test
    @DisplayName("존재하지 않는 포스트 ID로 태깅 시도시 예외가 발생한다")
    void 존재하지않는_포스트_태깅_예외발생() {
        // given
        Long nonExistentId = 999L;
        given(postRepositoryPort.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> autoTaggingService.autoTag(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Post not found: " + nonExistentId);

        verify(taggingProcessor, never()).processTagging(any());
    }

    @Test
    @DisplayName("LLM 태깅 실패시 FAILED 상태로 설정하고 예외를 발생시킨다")
    void LLM_태깅_실패_FAILED_상태_예외발생() {
        // given
        Long postId = 1L;
        Post post = createTestPost(postId);
        given(postRepositoryPort.findById(postId)).willReturn(Optional.of(post));

        given(taggingProcessor.processTagging(post))
                .willThrow(new RuntimeException("LLM API error"));

        // when & then
        assertThatThrownBy(() -> autoTaggingService.autoTag(postId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("LLM API error");

        verify(postRepositoryPort).updateTaggingStatus(postId, TaggingProcessStatus.FAILED);
        verify(tagPersistenceService, never()).persistTaggingResult(any(), any());
        verify(rejectedItemService, never()).saveRejectedItems(any(), any());
    }


    private Post createTestPost(Long id) {
        return Post.builder()
                .id(id)
                .title("테스트 포스트 " + id)
                .content("테스트 내용 " + id)
                .originalUrl("https://example.com/post/" + id)
                .normalizedUrl("https://example.com/post/" + id)
                .author("테스트 작가")
                .publishedAt(LocalDateTime.now())
                .taggingProcessStatus(TaggingProcessStatus.NOT_PROCESSED)
                .build();
    }

}