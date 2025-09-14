package com.techbloghub.output.rss.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

public class RssEnvironmentConfig {

    @Configuration
    @Profile("local")
    @PropertySource("classpath:rss-local.properties")
    static class LocalConfig {
    }

    @Configuration
    @Profile("docker")
    @PropertySource("classpath:rss-docker.properties")
    static class DockerConfig {
    }

    @Configuration
    @Profile("production")
    @PropertySource("classpath:rss-production.properties")
    static class ProductionConfig {
    }
}