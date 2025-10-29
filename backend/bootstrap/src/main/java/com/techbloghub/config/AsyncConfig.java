package com.techbloghub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 * Discord 알림 등 비동기 작업을 위한 설정
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring의 기본 AsyncConfigurer를 사용
    // application.yml의 spring.task.execution 설정이 적용됨
}
