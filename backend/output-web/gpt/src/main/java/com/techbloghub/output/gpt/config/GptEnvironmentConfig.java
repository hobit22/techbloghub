package com.techbloghub.output.gpt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

public class GptEnvironmentConfig {

    @Configuration
    @Profile("local")
    @PropertySource("classpath:gpt-local.properties")
    static class LocalConfig {
    }

    @Configuration
    @Profile("docker")
    @PropertySource("classpath:gpt-docker.properties")
    static class DockerConfig {
    }

    @Configuration
    @Profile("production")
    @PropertySource("classpath:gpt-production.properties")
    static class ProductionConfig {
    }
}