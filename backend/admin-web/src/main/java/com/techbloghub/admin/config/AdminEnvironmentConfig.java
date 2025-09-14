package com.techbloghub.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

public class AdminEnvironmentConfig {

    @Configuration
    @Profile("local")
    @PropertySource("classpath:admin-local.properties")
    static class LocalConfig {
    }

    @Configuration
    @Profile("docker")
    @PropertySource("classpath:admin-docker.properties")
    static class DockerConfig {
    }

    @Configuration
    @Profile("production")
    @PropertySource("classpath:admin-production.properties")
    static class ProductionConfig {
    }
}