#!/bin/bash

# AI Web Crawler Shell Script
# Usage: ./run-crawler.sh <config-file.json>

JAR_FILE="target/ai-crawler-1.0.0-exec.jar"
CONFIG_FILE="$1"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven first."
    exit 1
fi

# Check if Java 21 is installed
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
if [[ "$JAVA_VERSION" != "21"* ]]; then
    echo "Error: Java 21 is required. Current version: $JAVA_VERSION"
    exit 1
fi

# Build the project if JAR doesn't exist
if [ ! -f "$JAR_FILE" ]; then
    echo "Building project..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "Error: Build failed"
        exit 1
    fi
fi

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file '$CONFIG_FILE' not found"
    echo "Usage: $0 <config-file.json>"
    echo "Sample config: config/sample-config.json"
    exit 1
fi

# Run the crawler
echo "Starting web crawler with config: $CONFIG_FILE"
java -jar "$JAR_FILE" "$CONFIG_FILE"

if [ $? -eq 0 ]; then
    echo "Crawling completed successfully!"
else
    echo "Crawling failed!"
    exit 1
fi