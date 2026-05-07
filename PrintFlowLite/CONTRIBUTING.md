# Contributing to PrintFlowLite

Thank you for your interest in contributing to PrintFlowLite!

## Development Setup

### Prerequisites

- JDK 11 or higher
- Maven 3.6+
- Git
- (Optional) PostgreSQL for development
- (Optional) CUPS development headers (for cups-notifier)

### Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/PrintFlowLite.git`
3. Import into your IDE as a Maven project
4. Build: `mvn clean install -DskipTests`

### Building Individual Packages

```bash
# Build common library first
cd packages/common
mvn clean install -DskipTests

# Build core (depends on common)
cd ../core
mvn clean install -DskipTests

# Build server (depends on core and ext)
cd ../server
mvn clean install -DskipTests

# Build client (depends on common)
cd ../client
mvn clean install -DskipTests
```

### Code Style

- Follow standard Java naming conventions
- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- See `.editorconfig` for formatting rules

### Pull Request Process

1. Create a feature branch from `develop`
2. Make your changes
3. Ensure all tests pass
4. Update documentation as needed
5. Submit a pull request

### License

By contributing, you agree that your contributions will be licensed under the GNU Affero General Public License (AGPL) v3.
