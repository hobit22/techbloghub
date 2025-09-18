package com.techbloghub.domain.crawling.service;

import com.techbloghub.domain.blog.model.Blog;
import com.techbloghub.domain.blog.model.BlogStatus;
import com.techbloghub.domain.crawling.model.CrawlingResult;
import com.techbloghub.domain.crawling.model.RssEntry;
import com.techbloghub.domain.crawling.model.RssFeed;
import com.techbloghub.domain.blog.port.BlogRepositoryPort;
import com.techbloghub.domain.crawling.port.FetchRssPort;
import com.techbloghub.domain.post.port.PostRepositoryPort;
import com.techbloghub.domain.post.model.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RssCrawlerServiceTest {

    @Mock
    private BlogRepositoryPort blogRepositoryPort;

    @Mock
    private FetchRssPort fetchRssPort;

    @Mock
    private PostRepositoryPort postRepositoryPort;

    @InjectMocks
    private RssCrawlerService rssCrawlerService;

    @Test
    @DisplayName("활성 블로그들에 대해 RSS 크롤링을 성공적으로 실행한다")
    void 활성_블로그_RSS_크롤링_성공() {
        // given
        Blog activeBlog1 = createBlog(1L, "활성 블로그1", BlogStatus.ACTIVE, "https://blog1.com/rss");
        Blog activeBlog2 = createBlog(2L, "활성 블로그2", BlogStatus.ACTIVE, "https://blog2.com/rss");
        Blog inactiveBlog = createBlog(3L, "비활성 블로그", BlogStatus.INACTIVE, "https://blog3.com/rss");

        List<Blog> allBlogs = List.of(activeBlog1, activeBlog2, inactiveBlog);
        given(blogRepositoryPort.findAll()).willReturn(allBlogs);

        RssFeed validFeed = createValidRssFeed("https://blog1.com/rss", 2);
        given(fetchRssPort.fetchRssFeed("https://blog1.com/rss")).willReturn(validFeed);
        given(fetchRssPort.fetchRssFeed("https://blog2.com/rss")).willReturn(validFeed);

        given(postRepositoryPort.existsByNormalizedUrl(anyString())).willReturn(false);

        // when
        CrawlingResult result = rssCrawlerService.crawlAllActiveBlogs();

        // then
        assertThat(result.getTotalBlogs()).isEqualTo(2); // 활성 블로그만
        assertThat(result.getProcessedBlogs()).isEqualTo(2);
        assertThat(result.getTotalPostsSaved()).isEqualTo(4); // 각 블로그당 2개씩
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getErrors()).isEmpty();

        verify(fetchRssPort).setTimeout(30);
        verify(postRepositoryPort, times(4)).savePost(any(Post.class));
        verify(blogRepositoryPort).resetFailureCount(1L);
        verify(blogRepositoryPort).resetFailureCount(2L);
        verify(blogRepositoryPort).updateLastCrawledAt(eq(1L), any(LocalDateTime.class));
        verify(blogRepositoryPort).updateLastCrawledAt(eq(2L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("특정 블로그에 대해 RSS 크롤링을 성공적으로 실행한다")
    void 특정_블로그_RSS_크롤링_성공() {
        // given
        Long blogId = 1L;
        Blog blog = createBlog(blogId, "테스트 블로그", BlogStatus.ACTIVE, "https://blog.com/rss");
        given(blogRepositoryPort.findById(blogId)).willReturn(Optional.of(blog));

        RssFeed validFeed = createValidRssFeed("https://blog.com/rss", 3);
        given(fetchRssPort.fetchRssFeed("https://blog.com/rss")).willReturn(validFeed);
        given(postRepositoryPort.existsByNormalizedUrl(anyString())).willReturn(false);

        // when
        CrawlingResult result = rssCrawlerService.crawlSpecificBlog(blogId);

        // then
        assertThat(result.getTotalBlogs()).isEqualTo(1);
        assertThat(result.getProcessedBlogs()).isEqualTo(1);
        assertThat(result.getTotalPostsSaved()).isEqualTo(3);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getBlogResults()).hasSize(1);
        assertThat(result.getBlogResults().get(0).getBlogId()).isEqualTo(blogId);
        assertThat(result.getBlogResults().get(0).getPostsSaved()).isEqualTo(3);

        verify(postRepositoryPort, times(3)).savePost(any(Post.class));
        verify(blogRepositoryPort).resetFailureCount(blogId);
        verify(blogRepositoryPort).updateLastCrawledAt(eq(blogId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("존재하지 않는 블로그 ID로 크롤링 시도시 예외가 발생한다")
    void 존재하지않는_블로그_크롤링_예외발생() {
        // given
        Long nonExistentId = 999L;
        given(blogRepositoryPort.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> rssCrawlerService.crawlSpecificBlog(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Blog not found: " + nonExistentId);

        verify(fetchRssPort, never()).fetchRssFeed(anyString());
    }

    @Test
    @DisplayName("유효하지 않은 RSS URL을 가진 블로그는 건너뛰고 에러로 기록한다")
    void 유효하지않은_RSS_URL_블로그_건너뛰기() {
        // given
        Blog blogWithInvalidUrl = createBlog(1L, "잘못된 URL 블로그", BlogStatus.ACTIVE, "invalid-url");
        Blog validBlog = createBlog(2L, "정상 블로그", BlogStatus.ACTIVE, "https://blog.com/rss");

        List<Blog> blogs = List.of(blogWithInvalidUrl, validBlog);
        given(blogRepositoryPort.findAll()).willReturn(blogs);

        RssFeed validFeed = createValidRssFeed("https://blog.com/rss", 1);
        given(fetchRssPort.fetchRssFeed("https://blog.com/rss")).willReturn(validFeed);
        given(postRepositoryPort.existsByNormalizedUrl(anyString())).willReturn(false);

        // when
        CrawlingResult result = rssCrawlerService.crawlAllActiveBlogs();

        // then
        assertThat(result.getTotalBlogs()).isEqualTo(2);
        assertThat(result.getProcessedBlogs()).isEqualTo(1); // 정상 블로그만 처리
        assertThat(result.getTotalPostsSaved()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getBlogId()).isEqualTo(1L);
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("INVALID_RSS_URL");
        assertThat(result.isPartiallySuccessful()).isTrue();

        verify(fetchRssPort, never()).fetchRssFeed("invalid-url");
        verify(fetchRssPort).fetchRssFeed("https://blog.com/rss");
    }

    @Test
    @DisplayName("RSS 피드 가져오기 실패시 에러로 기록하고 계속 진행한다")
    void RSS_피드_가져오기_실패_에러기록() {
        // given
        Blog failingBlog = createBlog(1L, "실패 블로그", BlogStatus.ACTIVE, "https://failing.com/rss");
        Blog successBlog = createBlog(2L, "성공 블로그", BlogStatus.ACTIVE, "https://success.com/rss");

        List<Blog> blogs = List.of(failingBlog, successBlog);
        given(blogRepositoryPort.findAll()).willReturn(blogs);

        given(fetchRssPort.fetchRssFeed("https://failing.com/rss"))
                .willThrow(new RuntimeException("Connection timeout"));

        RssFeed validFeed = createValidRssFeed("https://success.com/rss", 1);
        given(fetchRssPort.fetchRssFeed("https://success.com/rss")).willReturn(validFeed);
        given(postRepositoryPort.existsByNormalizedUrl(anyString())).willReturn(false);

        // when
        CrawlingResult result = rssCrawlerService.crawlAllActiveBlogs();

        // then
        assertThat(result.getTotalBlogs()).isEqualTo(2);
        assertThat(result.getProcessedBlogs()).isEqualTo(1); // 성공 블로그만
        assertThat(result.getTotalPostsSaved()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getBlogId()).isEqualTo(1L);
        assertThat(result.getErrors().get(0).getErrorType()).isEqualTo("RuntimeException");
        assertThat(result.isPartiallySuccessful()).isTrue();

        verify(blogRepositoryPort).incrementFailureCount(1L);
        verify(blogRepositoryPort).resetFailureCount(2L);
    }

    @Test
    @DisplayName("중복된 포스트는 건너뛰고 저장하지 않는다")
    void 중복_포스트_건너뛰기() {
        // given
        Blog blog = createBlog(1L, "테스트 블로그", BlogStatus.ACTIVE, "https://blog.com/rss");
        given(blogRepositoryPort.findById(1L)).willReturn(Optional.of(blog));

        RssFeed feedWithDuplicates = createValidRssFeed("https://blog.com/rss", 3);
        given(fetchRssPort.fetchRssFeed("https://blog.com/rss")).willReturn(feedWithDuplicates);

        // 첫 번째와 세 번째 포스트는 중복, 두 번째는 신규
        given(postRepositoryPort.existsByNormalizedUrl(anyString()))
                .willReturn(true, false, true);

        // when
        CrawlingResult result = rssCrawlerService.crawlSpecificBlog(1L);

        // then
        assertThat(result.getTotalPostsSaved()).isEqualTo(1); // 신규 포스트 1개만 저장
        assertThat(result.getBlogResults().get(0).getTotalPostsFound()).isEqualTo(3);
        assertThat(result.getBlogResults().get(0).getPostsSaved()).isEqualTo(1);

        verify(postRepositoryPort, times(1)).savePost(any(Post.class)); // 1번만 호출
    }

    @Test
    @DisplayName("포스트 저장 실패시 로그만 남기고 계속 진행한다")
    void 포스트_저장_실패_계속진행() {
        // given
        Blog blog = createBlog(1L, "테스트 블로그", BlogStatus.ACTIVE, "https://blog.com/rss");
        given(blogRepositoryPort.findById(1L)).willReturn(Optional.of(blog));

        RssFeed validFeed = createValidRssFeed("https://blog.com/rss", 2);
        given(fetchRssPort.fetchRssFeed("https://blog.com/rss")).willReturn(validFeed);
        given(postRepositoryPort.existsByNormalizedUrl(anyString())).willReturn(false);

        // 첫 번째 포스트 저장 실패, 두 번째는 성공하도록 설정
        AtomicInteger callCount = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (callCount.incrementAndGet() == 1) {
                throw new RuntimeException("DB error");
            }
            return null; // 성공
        }).when(postRepositoryPort).savePost(any(Post.class));

        // when
        CrawlingResult result = rssCrawlerService.crawlSpecificBlog(1L);

        // then
        assertThat(result.getTotalPostsSaved()).isEqualTo(1); // 성공한 1개만 카운트
        assertThat(result.getBlogResults().get(0).getTotalPostsFound()).isEqualTo(2);
        assertThat(result.getBlogResults().get(0).getPostsSaved()).isEqualTo(1);

        verify(postRepositoryPort, times(2)).savePost(any(Post.class));
    }

    @Test
    @DisplayName("모든 블로그가 비활성 상태인 경우 아무것도 처리하지 않는다")
    void 모든_블로그_비활성_아무것도_처리안함() {
        // given
        Blog inactiveBlog1 = createBlog(1L, "비활성 블로그1", BlogStatus.INACTIVE, "https://blog1.com/rss");
        Blog inactiveBlog2 = createBlog(2L, "비활성 블로그2", BlogStatus.INACTIVE, "https://blog2.com/rss");

        List<Blog> blogs = List.of(inactiveBlog1, inactiveBlog2);
        given(blogRepositoryPort.findAll()).willReturn(blogs);

        // when
        CrawlingResult result = rssCrawlerService.crawlAllActiveBlogs();

        // then
        assertThat(result.getTotalBlogs()).isEqualTo(0);
        assertThat(result.getProcessedBlogs()).isEqualTo(0);
        assertThat(result.getTotalPostsSaved()).isEqualTo(0);
        assertThat(result.getBlogResults()).isEmpty();
        assertThat(result.getErrors()).isEmpty();

        verify(fetchRssPort, never()).fetchRssFeed(anyString());
        verify(postRepositoryPort, never()).savePost(any(Post.class));
    }

    private Blog createBlog(Long id, String name, BlogStatus status, String rssUrl) {
        return Blog.builder()
                .id(id)
                .name(name)
                .company("테스트 회사")
                .rssUrl(rssUrl)
                .siteUrl("https://example.com")
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private RssFeed createValidRssFeed(String url, int entryCount) {
        List<RssEntry> entries = java.util.stream.IntStream.range(0, entryCount)
                .mapToObj(i -> RssEntry.builder()
                        .title("테스트 포스트 " + (i + 1))
                        .url("https://example.com/post/" + (i + 1))
                        .content("테스트 내용 " + (i + 1))
                        .author("테스트 작가")
                        .publishedAt(LocalDateTime.now().minusDays(i))
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return RssFeed.builder()
                .url(url)
                .title("테스트 피드")
                .description("테스트 피드 설명")
                .entries(entries)
                .build();
    }
}