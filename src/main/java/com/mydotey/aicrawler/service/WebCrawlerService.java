package com.mydotey.aicrawler.service;

import com.mydotey.aicrawler.config.CrawlConfig;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WebCrawlerService {
    private static final Logger log = LoggerFactory.getLogger(WebCrawlerService.class);
    private final Map<String, Boolean> visitedUrls = new ConcurrentHashMap<>();
    private final AtomicInteger crawledCount = new AtomicInteger(0);

    public void crawl(CrawlConfig config) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            // Set cookies if provided
            if (config.getCookies() != null && !config.getCookies().isEmpty()) {
                // Create cookies using BrowserContext.addCookies which accepts a list of Cookie
                // objects
                String domain = extractDomain(config.getStartUrls().get(0));
                List<com.microsoft.playwright.options.Cookie> cookies = new ArrayList<>();
                config.getCookies().forEach((name, value) -> {
                    cookies.add(new com.microsoft.playwright.options.Cookie(name, value)
                            .setDomain(domain)
                            .setPath("/"));
                });
                context.addCookies(cookies);
            }

            // Create output directory
            Path outputDir = Paths.get(config.getOutput());
            Files.createDirectories(outputDir);

            // Start crawling from each start URL
            for (String startUrl : config.getStartUrls()) {
                if (crawledCount.get() >= config.getMaxCount()) {
                    break;
                }
                crawlUrl(context, startUrl, config, 0, outputDir);
            }
        } catch (Exception e) {
            log.error("Crawling failed", e);
        }
    }

    private void crawlUrl(BrowserContext context, String url, CrawlConfig config, int currentDepth, Path outputDir) {
        if (crawledCount.get() >= config.getMaxCount() || currentDepth > config.getDepth()) {
            return;
        }

        String normalizedUrl = normalizeUrl(url, config);
        if (visitedUrls.putIfAbsent(normalizedUrl, true) != null) {
            return; // Already visited
        }

        if (shouldExclude(url, config)) {
            return;
        }

        try (Page page = context.newPage()) {
            page.setViewportSize(1920, 1080);

            log.info("Crawling: {} (depth: {})", url, currentDepth);
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Generate PDF
            String title = page.title();
            String fileName = generateFileName(title, crawledCount.get());
            Path pdfPath = outputDir.resolve(fileName);

            page.pdf(new Page.PdfOptions()
                    .setPath(pdfPath)
                    .setFormat("A4")
                    .setPrintBackground(true));

            log.info("Saved PDF: {}", pdfPath);
            crawledCount.incrementAndGet();

            // Crawl nested links if within depth limit
            if (currentDepth < config.getDepth()) {
                List<String> links = page.querySelectorAll("a")
                        .stream()
                        .map(element -> element.getAttribute("href"))
                        .filter(Objects::nonNull)
                        .filter(link -> isSameHost(link, url))
                        .filter(link -> !shouldExclude(link, config))
                        .distinct()
                        .toList();

                for (String link : links) {
                    if (crawledCount.get() >= config.getMaxCount()) {
                        break;
                    }
                    crawlUrl(context, makeAbsoluteUrl(link, url), config, currentDepth + 1, outputDir);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to crawl {}: {}", url, e.getMessage());
        }
    }

    private String normalizeUrl(String url, CrawlConfig config) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            } else if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
                if (path.isEmpty()) {
                    path = "/";
                }
            }

            // Handle query parameters based on config
            String query = null;
            if (config.getParams() != null && !config.getParams().isEmpty()) {
                // Include only specified query parameters
                Map<String, List<String>> filteredParams = new TreeMap<>();
                String originalQuery = uri.getQuery();
                if (originalQuery != null) {
                    String[] pairs = originalQuery.split("&");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=", 2);
                        String key = keyValue[0];
                        if (config.getParams().contains(key)) {
                            String value = keyValue.length > 1 ? keyValue[1] : "";
                            filteredParams.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                        }
                    }

                    StringBuilder newQuery = new StringBuilder();
                    for (Map.Entry<String, List<String>> entry : filteredParams.entrySet()) {
                        for (String value : entry.getValue()) {
                            if (newQuery.length() > 0) {
                                newQuery.append("&");
                            }
                            newQuery.append(entry.getKey()).append("=").append(value);
                        }
                    }
                    query = newQuery.toString();
                }
            }

            // Don't preserve scheme (ignore it)
            // Use filtered query parameters if specified in config
            return new URI(null, null, uri.getHost(), uri.getPort(),
                    path, query, null).toString();
        } catch (URISyntaxException e) {
            return url; // Return original if normalization fails
        }
    }

    private boolean shouldExclude(String url, CrawlConfig config) {
        // Exclude non-HTTP protocols like mailto:, tel:, etc.
        if (url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("javascript:")) {
            return true;
        }

        if (config.getExcludeUrls() == null) {
            return false;
        }
        return config.getExcludeUrls().stream().anyMatch(url::contains);
    }

    private boolean isSameHost(String link, String baseUrl) {
        try {
            URI linkUri = new URI(link);
            URI baseUri = new URI(baseUrl);

            // Handle relative URLs
            if (linkUri.getHost() == null) {
                return true; // Relative URLs are same host
            }

            return linkUri.getHost().equals(baseUri.getHost());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private String makeAbsoluteUrl(String link, String baseUrl) {
        try {
            URI baseUri = new URI(baseUrl);
            URI linkUri = new URI(link);

            if (linkUri.isAbsolute()) {
                return link;
            }

            return baseUri.resolve(linkUri).toString();
        } catch (URISyntaxException e) {
            return link;
        }
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private String generateFileName(String title, int count) {
        // Clean title for filename
        String cleanTitle = title.replaceAll("[^a-zA-Z0-9\u4e00-\u9fff]", "_")
                .replaceAll("_+", "_")
                .trim();

        if (cleanTitle.isEmpty()) {
            cleanTitle = "untitled";
        }

        // Limit title length
        if (cleanTitle.length() > 50) {
            cleanTitle = cleanTitle.substring(0, 50);
        }

        return String.format("%s_%d.pdf", cleanTitle, count);
    }
}