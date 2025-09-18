package com.techbloghub.domain.blog.service;

import com.techbloghub.domain.blog.model.Blog;
import com.techbloghub.domain.blog.model.BlogStatus;
import com.techbloghub.domain.blog.port.BlogRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogRepositoryPort blogRepositoryPort;

    @InjectMocks
    private BlogService blogService;

    @Test
    @DisplayName("페이지네이션으로 모든 블로그를 조회한다")
    void 모든_블로그_페이지네이션_조회_성공() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Blog> blogs = List.of(
                createBlog(1L, "블로그1", BlogStatus.ACTIVE),
                createBlog(2L, "블로그2", BlogStatus.INACTIVE)
        );
        Page<Blog> blogPage = new PageImpl<>(blogs, pageable, blogs.size());
        given(blogRepositoryPort.findAll(pageable)).willReturn(blogPage);

        // when
        Page<Blog> result = blogService.getAllBlogs(pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("블로그1");
        assertThat(result.getContent().get(1).getName()).isEqualTo("블로그2");
        verify(blogRepositoryPort).findAll(pageable);
    }

    @Test
    @DisplayName("활성 상태의 블로그만 조회한다")
    void 활성_블로그만_조회_성공() {
        // given
        List<Blog> allBlogs = List.of(
                createBlog(1L, "활성 블로그1", BlogStatus.ACTIVE),
                createBlog(2L, "비활성 블로그", BlogStatus.INACTIVE),
                createBlog(3L, "활성 블로그2", BlogStatus.ACTIVE)
        );
        given(blogRepositoryPort.findAll()).willReturn(allBlogs);

        // when
        List<Blog> activeBlogs = blogService.getActiveBlogs();

        // then
        assertThat(activeBlogs).hasSize(2);
        assertThat(activeBlogs).allMatch(Blog::isActive);
        assertThat(activeBlogs.get(0).getName()).isEqualTo("활성 블로그1");
        assertThat(activeBlogs.get(1).getName()).isEqualTo("활성 블로그2");
        verify(blogRepositoryPort).findAll();
    }

    @Test
    @DisplayName("모든 블로그가 비활성 상태인 경우 빈 목록을 반환한다")
    void 모든_블로그_비활성_빈목록_반환() {
        // given
        List<Blog> allBlogs = List.of(
                createBlog(1L, "비활성 블로그1", BlogStatus.INACTIVE),
                createBlog(2L, "비활성 블로그2", BlogStatus.INACTIVE)
        );
        given(blogRepositoryPort.findAll()).willReturn(allBlogs);

        // when
        List<Blog> activeBlogs = blogService.getActiveBlogs();

        // then
        assertThat(activeBlogs).isEmpty();
        verify(blogRepositoryPort).findAll();
    }

    @Test
    @DisplayName("ID로 블로그를 조회한다")
    void ID로_블로그_조회_성공() {
        // given
        Long blogId = 1L;
        Blog blog = createBlog(blogId, "테스트 블로그", BlogStatus.ACTIVE);
        given(blogRepositoryPort.findById(blogId)).willReturn(Optional.of(blog));

        // when
        Optional<Blog> result = blogService.getBlogById(blogId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(blogId);
        assertThat(result.get().getName()).isEqualTo("테스트 블로그");
        verify(blogRepositoryPort).findById(blogId);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 블로그 조회시 빈 Optional을 반환한다")
    void 존재하지않는_ID_블로그_조회_빈Optional_반환() {
        // given
        Long nonExistentId = 999L;
        given(blogRepositoryPort.findById(nonExistentId)).willReturn(Optional.empty());

        // when
        Optional<Blog> result = blogService.getBlogById(nonExistentId);

        // then
        assertThat(result).isEmpty();
        verify(blogRepositoryPort).findById(nonExistentId);
    }

    @Test
    @DisplayName("존재하는 블로그의 통계를 조회한다")
    void 블로그_통계_조회_성공() {
        // given
        Long blogId = 1L;
        LocalDateTime lastCrawledAt = LocalDateTime.now().minusHours(2);
        Blog blog = createBlogWithCrawledAt(blogId, "테스트 블로그", BlogStatus.ACTIVE, lastCrawledAt);
        given(blogRepositoryPort.findById(blogId)).willReturn(Optional.of(blog));

        // when
        Map<String, Object> stats = blogService.getBlogStats(blogId);

        // then
        assertThat(stats).containsEntry("blogId", blogId);
        assertThat(stats).containsEntry("blogName", "테스트 블로그");
        assertThat(stats).containsEntry("totalPosts", 0);
        assertThat(stats).containsEntry("lastCrawledAt", lastCrawledAt);
        assertThat(stats).containsEntry("status", BlogStatus.ACTIVE);
        verify(blogRepositoryPort).findById(blogId);
    }

    @Test
    @DisplayName("null ID로 블로그 통계 조회시 예외가 발생한다")
    void null_ID_블로그_통계_조회_예외발생() {
        // when & then
        assertThatThrownBy(() -> blogService.getBlogStats(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("블로그 ID는 필수입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 블로그의 통계 조회시 예외가 발생한다")
    void 존재하지않는_블로그_통계_조회_예외발생() {
        // given
        Long nonExistentId = 999L;
        given(blogRepositoryPort.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> blogService.getBlogStats(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 블로그입니다: ID=" + nonExistentId);
        verify(blogRepositoryPort).findById(nonExistentId);
    }

    @Test
    @DisplayName("유효한 정보로 새 블로그를 생성한다")
    void 새_블로그_생성_성공() {
        // given
        String name = "새 블로그";
        String company = "테스트 회사";
        String rssUrl = "https://example.com/rss";
        String siteUrl = "https://example.com";
        String logoUrl = "https://example.com/logo.png";
        String description = "테스트 설명";

        Blog expectedBlog = Blog.of(name, company, rssUrl, siteUrl, logoUrl, description);
        Blog savedBlog = createBlog(1L, name, BlogStatus.ACTIVE);
        given(blogRepositoryPort.save(any(Blog.class))).willReturn(savedBlog);

        // when
        Blog result = blogService.createBlog(name, company, rssUrl, siteUrl, logoUrl, description);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getStatus()).isEqualTo(BlogStatus.ACTIVE);
        verify(blogRepositoryPort).save(any(Blog.class));
    }

    @Test
    @DisplayName("공백이 포함된 정보로 블로그 생성시 정상적으로 트림된다")
    void 공백포함_정보로_블로그_생성_트림_성공() {
        // given
        String name = "  새 블로그  ";
        String company = "  테스트 회사  ";
        String rssUrl = "  https://example.com/rss  ";
        String siteUrl = "  https://example.com  ";
        String logoUrl = "  https://example.com/logo.png  ";
        String description = "  테스트 설명  ";

        Blog savedBlog = createBlog(1L, "새 블로그", BlogStatus.ACTIVE);
        given(blogRepositoryPort.save(any(Blog.class))).willReturn(savedBlog);

        // when
        Blog result = blogService.createBlog(name, company, rssUrl, siteUrl, logoUrl, description);

        // then
        assertThat(result.getName()).isEqualTo("새 블로그");
        verify(blogRepositoryPort).save(any(Blog.class));
    }

    private Blog createBlog(Long id, String name, BlogStatus status) {
        return Blog.builder()
                .id(id)
                .name(name)
                .company("테스트 회사")
                .rssUrl("https://example.com/rss")
                .siteUrl("https://example.com")
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Blog createBlogWithCrawledAt(Long id, String name, BlogStatus status, LocalDateTime lastCrawledAt) {
        return Blog.builder()
                .id(id)
                .name(name)
                .company("테스트 회사")
                .rssUrl("https://example.com/rss")
                .siteUrl("https://example.com")
                .status(status)
                .lastCrawledAt(lastCrawledAt)
                .createdAt(LocalDateTime.now())
                .build();
    }
}