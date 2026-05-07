# PrintFlowLite Maven Wrapper

This directory contains Maven wrapper files for easy building without requiring Maven to be installed globally.

## Usage

```bash
# Linux/macOS
./mvnw clean install -DskipTests

# Windows
mvnw.cmd clean install -DskipTests
```

## Generate Maven Wrapper

If you need to regenerate the Maven wrapper:

```bash
mvn wrapper:wrapper
```
