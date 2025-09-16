package com.techbloghub.persistence.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Set;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofDays(1))  // TTL 1일
                .maximumSize(10000)                     // 최대 10,000개 항목
                .recordStats());                        // 캐시 통계 수집

        // 캐시 이름 등록
        cacheManager.setCacheNames(Set.of("categories", "tags", "tagSearch", "allBlogs"));

        return cacheManager;
    }
}