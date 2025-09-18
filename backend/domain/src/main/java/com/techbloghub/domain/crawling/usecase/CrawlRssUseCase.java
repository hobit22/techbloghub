package com.techbloghub.domain.crawling.usecase;

import com.techbloghub.domain.crawling.model.CrawlingResult;

/**
 * RSS 크롤링 유스케이스 인터페이스
 */
public interface CrawlRssUseCase {

    /**
     * 모든 활성 블로그를 크롤링
     *
     * @return 크롤링 결과
     */
    CrawlingResult crawlAllActiveBlogs();

    /**
     * 특정 블로그를 크롤링
     *
     * @param blogId 크롤링할 블로그 ID
     * @return 크롤링 결과
     */
    CrawlingResult crawlSpecificBlog(Long blogId);
}