package com.mydotey.aicrawler;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class ChineseSupportTest {

    @Test
    void testChineseCharacterSupport() throws Exception {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.setViewportSize(1920, 1080);

            // Test with Chinese content
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>中文测试页面 - Chinese Test Page</title>
                    <style>
                        body { font-family: 'SimSun', '宋体', sans-serif; }
                        h1 { color: #333; }
                    </style>
                </head>
                <body>
                    <h1>中文标题 - Chinese Title</h1>
                    <p>这是一段中文文本内容，用于测试PDF生成中的中文支持。</p>
                    <p>This is English text for comparison.</p>
                    <p>混合文本: Mixed 中文 and English 内容</p>
                </body>
                </html>
                """;

            // Create temporary HTML file
            Path tempHtml = Files.createTempFile("chinese-test", ".html");
            Files.writeString(tempHtml, htmlContent);

            page.navigate("file://" + tempHtml.toAbsolutePath());
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Generate PDF
            Path pdfPath = Path.of("chinese-test-output.pdf");
            page.pdf(new Page.PdfOptions()
                    .setPath(pdfPath)
                    .setFormat("A4")
                    .setPrintBackground(true));

            System.out.println("PDF generated with Chinese content: " + pdfPath.toAbsolutePath());

            // Cleanup
            Files.deleteIfExists(tempHtml);
        }
    }
}