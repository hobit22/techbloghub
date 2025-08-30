package com.techbloghub.domain.port.in;

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
     * @param blogId 크롤링할 블로그 ID
     * @return 저장된 새로운 포스트 수
     */
    int crawlSpecificBlog(Long blogId);
}