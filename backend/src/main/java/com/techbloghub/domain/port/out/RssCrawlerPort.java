package com.techbloghub.domain.port.out;

import com.techbloghub.domain.model.Blog;
import com.techbloghub.domain.model.Post;

import java.util.List;

/**
 * RSS 크롤링 외부 서비스 아웃바운드 포트
 */
public interface RssCrawlerPort {
    
    /**
     * 블로그의 RSS 피드를 크롤링하여 포스트 목록 반환
     */
    List<Post> crawlFeed(Blog blog);
    
    /**
     * RSS URL의 유효성 검증
     */
    boolean isValidRssUrl(String rssUrl);
}