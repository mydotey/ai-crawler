package com.mydotey.aicrawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.aicrawler.config.CrawlConfig;
import com.mydotey.aicrawler.service.WebCrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class ApplicationCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationCommandLineRunner.class);
    private final WebCrawlerService webCrawlerService;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApplicationCommandLineRunner(WebCrawlerService webCrawlerService, ApplicationContext applicationContext) {
        this.webCrawlerService = webCrawlerService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length != 1) {
            log.error("Usage: java -jar ai-crawler.jar <config-file.json>");
            System.exit(1);
        }

        String configFile = args[0];
        try {
            String configContent = Files.readString(Paths.get(configFile));
            CrawlConfig config = objectMapper.readValue(configContent, CrawlConfig.class);

            log.info("Starting web crawler with config: {}", config);
            webCrawlerService.crawl(config);
            log.info("Crawling completed successfully");

        } catch (Exception e) {
            log.error("Failed to read config file or execute crawling", e);
            System.exit(1);
        } finally {
            try {
                webCrawlerService.close();
            } catch (Exception e) {
                log.warn("Failed to close WebCrawlerService: {}", e.getMessage());
            } finally {
                // Explicitly shut down Spring context to ensure program exits
                System.exit(SpringApplication.exit(applicationContext));
            }
        }
    }
}