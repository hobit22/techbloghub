package com.techbloghub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI techBlogHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TechBlogHub API")
                        .description("기술블로그 모음 서비스 REST API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TechBlogHub Team")
                                .email("contact@techbloghub.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("https://api.techbloghub.com")
                                .description("Production server")))
                .tags(List.of(
                        new Tag()
                                .name("blogs")
                                .description("기술블로그 관리 API"),
                        new Tag()
                                .name("posts")
                                .description("블로그 포스트 조회 및 검색 API"),
                        new Tag()
                                .name("categories")
                                .description("카테고리 관리 API"),
                        new Tag()
                                .name("tags")
                                .description("태그 관리 API"),
                        new Tag()
                                .name("search")
                                .description("검색 기능 API")));
    }
}