# AI Web Crawler

A Java-based web crawler using Spring Boot and Playwright to crawl web pages and save them as PDF files with preserved styling and images.

## Features

- ✅ Command line tool with JSON configuration
- ✅ Cookie-based authentication support
- ✅ Depth-limited nested link crawling
- ✅ URL exclusion patterns
- ✅ PDF generation with original styling and images
- ✅ Chinese character support
- ✅ URL normalization and deduplication
- ✅ Same-host restriction for security

## Requirements

- Java 21+
- Maven
- Playwright browsers (automatically installed)

## Configuration

Create a JSON config file:

```json
{
  "cookies": {
    "session_id": "your_session_token"
  },
  "startUrls": [
    "https://example.com"
  ],
  "excludeUrls": [
    "/logout",
    "/admin"
  ],
  "depth": 2,
  "maxCount": 50,
  "output": "./output"
}
```

## Usage

The os should install the dep to run the browser:

```bash
sudo apt-get install libavif16 
```

1. **Build the project:**

   ```bash
   mvn clean package
   ```

2. **Run with shell script:**

   ```bash
   ./run-crawler.sh config/sample-config.json
   ```

3. **Or run directly with Java:**

   ```bash
   java -jar target/ai-crawler-1.0.0.jar your-config.json
   ```

## Output

PDF files are saved in the specified output directory with filenames like:
`Page_Title_123.pdf`

- Titles are cleaned for filename compatibility
- Chinese characters are supported in filenames and content
- Each file has a unique numeric suffix

## Technical Details

- **Framework**: Spring Boot 3.5.6
- **Browser Automation**: Playwright for Java
- **PDF Generation**: Native Playwright PDF support
- **URL Handling**: Java URI with parameter normalization
- **Concurrency**: Thread-safe crawling with visit tracking

## Sample Config

See `config/sample-config.json` for a complete example.

## Development

```bash
# Run tests
mvn test

# Build without tests
mvn package -DskipTests
```
