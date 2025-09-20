#!/bin/bash

# AI Web Crawler Shell Script
# Usage: ./run-crawler.sh <config-file.json>

set -euo pipefail

JAR_FILE="target/ai-crawler-1.0.0-exec.jar"
CONFIG_FILE="${1:-}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed. Please install Maven first."
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    print_error "Java is not installed. Please install Java first."
    exit 1
fi

# Check if Java 21 is installed
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
if [[ "$JAVA_VERSION" != "21"* ]]; then
    print_error "Java 21 is required. Current version: $JAVA_VERSION"
    exit 1
fi

# Validate config file argument
if [ -z "$CONFIG_FILE" ]; then
    print_error "No config file specified"
    echo "Usage: $0 <config-file.json>"
    echo "Sample config: config/sample-config.json"
    exit 1
fi

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    print_error "Config file '$CONFIG_FILE' not found"

    # Check if it's a relative path from config directory
    if [ -f "config/$CONFIG_FILE" ]; then
        CONFIG_FILE="config/$CONFIG_FILE"
        print_info "Found config file at: $CONFIG_FILE"
    else
        echo "Usage: $0 <config-file.json>"
        echo "Sample config: config/sample-config.json"
        exit 1
    fi
fi

# Always clean and build the project
print_info "Building project..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    print_error "Build failed"
    exit 1
fi
print_success "Build completed"

# Run the crawler
print_info "Starting web crawler with config: $CONFIG_FILE"
java -jar "$JAR_FILE" "$CONFIG_FILE"

if [ $? -eq 0 ]; then
    print_success "Crawling completed successfully!"
else
    print_error "Crawling failed!"
    exit 1
fi