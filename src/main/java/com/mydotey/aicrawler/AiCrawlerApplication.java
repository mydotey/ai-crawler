package com.mydotey.aicrawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;

@SpringBootApplication
public class AiCrawlerApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AiCrawlerApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }
}