package com.techbloghub.domain.crawling.model;

import com.techbloghub.domain.blog.model.Blog;
import com.techbloghub.domain.post.model.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * RSS 엔트리 도메인 모델
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class RssEntry {

    private static final Pattern URL_PATTERN =
            Pattern.compile("^https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$");

    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final String title;
    private final String content;
    private final String url;
    private final String author;
    private final LocalDateTime publishedAt;

    /**
     * RSS 엔트리가 유효한지 검증 (크롤링 시점 기준)
     */
    public boolean isValid() {
        return hasValidTitle() && hasValidUrl();
    }

    /**
     * 제목이 유효한지 검증
     */
    public boolean hasValidTitle() {
        return title != null &&
                !title.trim().isEmpty() &&
                title.length() <= MAX_TITLE_LENGTH;
    }

    /**
     * URL이 유효한지 검증
     */
    public boolean hasValidUrl() {
        return url != null &&
                URL_PATTERN.matcher(url).matches();
    }


    /**
     * 콘텐츠를 지정된 길이로 제한
     */
    public String getTruncatedContent() {
        if (content == null) return null;

        String cleaned = content.replaceAll("<[^>]*>", "").trim(); // HTML 태그 제거

        if (cleaned.length() <= MAX_CONTENT_LENGTH) {
            return cleaned;
        }

        return cleaned.substring(0, MAX_CONTENT_LENGTH - 3) + "...";
    }

    /**
     * 유효한 게시 시간 반환 (null이면 현재 시간)
     */
    public LocalDateTime getValidPublishedAt() {
        return publishedAt != null ? publishedAt : LocalDateTime.now();
    }

    public Post toPost(Blog blog) {
        return Post.builder()
                .title(title)
                .content(getTruncatedContent())
                .originalUrl(url)
                .normalizedUrl(Post.normalizeUrl(url))
                .author(author)
                .publishedAt(getValidPublishedAt())
                .blog(blog)
                .build();
    }
}