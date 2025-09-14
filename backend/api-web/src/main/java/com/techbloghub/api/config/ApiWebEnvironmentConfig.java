package com.techbloghub.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

public class ApiWebEnvironmentConfig {

    @Configuration
    @Profile("local")
    @PropertySource("classpath:api-web-local.properties")
    static class LocalConfig {
    }

    @Configuration
    @Profile("docker")
    @PropertySource("classpath:api-web-docker.properties")
    static class DockerConfig {
    }

    @Configuration
    @Profile("production")
    @PropertySource("classpath:api-web-production.properties")
    static class ProductionConfig {
    }
}