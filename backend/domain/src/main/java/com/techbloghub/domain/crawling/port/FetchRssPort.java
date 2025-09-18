package com.techbloghub.domain.crawling.port;

import com.techbloghub.domain.crawling.model.RssFeed;

/**
 * RSS 피드 조회를 위한 아웃바운드 포트
 */
public interface FetchRssPort {

    /**
     * RSS 피드를 가져옴
     *
     * @param rssUrl RSS URL
     * @return RSS 피드 정보
     * @throws RssFetchException RSS 피드 조회 실패 시
     */
    RssFeed fetchRssFeed(String rssUrl);

    /**
     * RSS 피드 조회 시 타임아웃 설정
     *
     * @param timeoutSeconds 타임아웃 (초)
     */
    void setTimeout(int timeoutSeconds);

    /**
     * RSS 피드 조회 예외
     */
    class RssFetchException extends RuntimeException {
        public RssFetchException(String message) {
            super(message);
        }
        
        public RssFetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}