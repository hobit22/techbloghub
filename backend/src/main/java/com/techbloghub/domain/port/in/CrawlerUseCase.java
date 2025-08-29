package com.techbloghub.domain.port.in;

import com.techbloghub.domain.model.Blog;

import java.util.List;

/**
 * RSS 크롤링 관련 비즈니스 유스케이스 인터페이스
 */
public interface CrawlerUseCase {
    
    /**
     * 모든 활성 블로그를 크롤링
     */
    void crawlAllActiveBlogs();
    
    /**
     * 특정 블로그를 크롤링
     */
    void crawlSpecificBlog(Long blogId);
    
    /**
     * 크롤링이 필요한 블로그 목록 조회
     */
    List<Blog> getBlogsNeedingCrawl();
}