package com.techbloghub.batch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

public class BatchEnvironmentConfig {

    @Configuration
    @Profile("local")
    @PropertySource("classpath:batch-local.properties")
    static class LocalConfig {
    }

    @Configuration
    @Profile("docker")
    @PropertySource("classpath:batch-docker.properties")
    static class DockerConfig {
    }

    @Configuration
    @Profile("production")
    @PropertySource("classpath:batch-production.properties")
    static class ProductionConfig {
    }
}