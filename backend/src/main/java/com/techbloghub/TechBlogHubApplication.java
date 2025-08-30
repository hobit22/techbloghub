package com.techbloghub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TechBlogHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechBlogHubApplication.class, args);
    }
}