# PrintFlowLite Development Guide

This guide covers how to set up a development environment, build the project, and make code contributions.

---

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| JDK | 11 or higher | Java runtime and compilation |
| Maven | 3.6+ | Build automation |
| Git | any recent | Version control |
| IDE | — | Recommended: IntelliJ IDEA, Eclipse |
| PostgreSQL | 9.6+ | Development database (optional) |
| CUPS | 1.4+ | For proxy printing development |

### Recommended IDE Setup

#### IntelliJ IDEA

1. **Import the project** — File > Open > select `PrintFlowLite/pom.xml`
2. **Import as Maven project** — IntelliJ will detect the Maven structure
3. **Set JDK** — File > Project Structure > Project > SDK
4. **Enable annotation processing** — Wicket uses annotation processors

#### Eclipse

1. Generate Eclipse project files:
```bash
mvn eclipse:eclipse
```
2. **Import** — File > Import > Existing Projects into Workspace
3. **Install m2e** — Maven Integration for Eclipse plugin

---

## Project Structure

```
PrintFlowLite/
├── pom.xml                 # Root POM (monorepo aggregator)
├── docs/                   # Documentation
├── packages/
│   ├── common/             # Shared library (build first)
│   │   ├── pom.xml
│   │   └── src/
│   ├── core/               # Business logic & persistence
│   │   ├── pom.xml
│   │   └── src/
│   ├── ext/                # Extension interface (plugins)
│   │   ├── pom.xml
│   │   └── src/
│   ├── server/             # Web application
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/   # Wicket pages, REST, CometD
│   │       │   ├── resources/  # Config, i18n
│   │       │   └── webapp/ # HTML templates, CSS, JS
│   │       └── test/       # Unit and integration tests
│   ├── client/             # Desktop Swing client
│   │   ├── pom.xml
│   │   └── src/
│   ├── cups-notifier/      # C CUPS notifier (separate build)
│   │   ├── pom.xml
│   │   └── src/
│   └── ppd/                # PostScript driver files
│       └── src/
└── tmp/                    # Git bare repos (upstream)
```

---

## Building

### Full Build

```bash
mvn clean install -DskipTests
```

### Build Order

Packages must be built in this order due to dependencies:

```bash
# 1. Common library
cd packages/common
mvn clean install -DskipTests

# 2. Core library
cd ../core
mvn clean install -DskipTests

# 3. Extension interface
cd ../ext
mvn clean install -DskipTests

# 4. Server web app
cd ../server
mvn clean package -DskipTests

# 5. Client application
cd ../client
mvn clean package -DskipTests
```

### Build Profiles

Activate specific build profiles:

```bash
# Build server only
mvn clean install -P build-server

# Build client only
mvn clean install -P build-client

# Build all (default)
mvn clean install -P build-all
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for a specific package
cd packages/server
mvn test

# Run with coverage
mvn test jacoco:report
```

---

## Running the Server in Development

### Option 1: Embedded Jetty (Recommended)

```bash
cd packages/server
mvn clean package -DskipTests
java -jar target/printflowlite-server-1.7.0.war
```

### Option 2: Run from IDE

Run the embedded Jetty launcher class. In the server package, find the main class:

```
org.printflow.lite.server.jetty.ServerRunner
```

Run it with JVM arguments:
```
-Dlog4j.configuration=file:src/main/resources/setup/log4j.properties
```

### Option 3: External Servlet Container

Deploy `target/printflowlite-server-1.7.0.war` to Jetty, Tomcat, or another Servlet 4.0 container.

---

## Development Configuration

### server.properties

The main configuration file is at:

```
packages/server/src/main/resources/setup/server.properties
```

Key settings for development:

```properties
# Database (embedded Derby for dev)
db.driver=org.apache.derby.jdbc.EmbeddedDataSource
db.url=jdbc:derby:memory:pfl;create=true

# Server bind address
server.host=0.0.0.0
server.port=8632

# HTTPS (disable for local dev)
ssl.enabled=N

# Logging
log4j.rootLogger=DEBUG
```

### log4j.properties

Configure logging in:

```
packages/server/src/main/resources/setup/log4j.properties
```

### Database Setup

For development with PostgreSQL:

```bash
# Create database
sudo -u postgres createdb -O printflowlite printflowlite

# Update server.properties
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://localhost:5432/printflowlite
db.username=printflowlite
db.password=your-password
```

---

## Code Standards

### Java Conventions

- **Package naming**: `org.printflow.lite.<package>.<module>`
- **Class naming**: PascalCase (e.g., `UserService`, `PrintJobDao`)
- **Method naming**: camelCase (e.g., `getUserById`, `processPrintJob`)
- **Constant naming**: UPPER_SNAKE_CASE (e.g., `MAX_PAGES_PER_JOB`)
- **Indentation**: 4 spaces (no tabs)
- **Line length**: Max 120 characters

### Package Structure

Each package follows a layered structure:

```
org.printflow.lite.<module>/
├── api/            # Public interfaces and DTOs
├── impl/           # Implementations
├── dao/            # Data Access Objects
├── service/        # Business logic services
├── web/            # Web layer (Wicket pages, REST)
├── config/         # Configuration classes
└── util/           # Utility classes
```

### Wicket Page Conventions

- Pages extend `BasePage` or `AdminBasePage`
- Models use `PropertyModel`, `CompoundPropertyModel`, or `LoadableDetachableModel`
- Forms use `CompoundPropertyModel` for two-way binding
- Resources placed alongside Java files (PageName.html, PageName.css)

### JPA / Hibernate Conventions

- Entities use `@Entity`, `@Table`, `@Column` annotations
- Use `Long` for IDs (nullable for new entities)
- Use `BigDecimal` for monetary amounts
- Use `Instant` or `ZonedDateTime` for timestamps
- Relationships use lazy loading by default
- Use `@Transactional` on service layer methods

### REST API Conventions

- Endpoints use `/restful/v1/` prefix
- Return `Response<T>` with appropriate HTTP status
- Use `400 Bad Request` for validation errors
- Use `404 Not Found` for missing resources
- Include error details in response body

---

## Testing

### Unit Tests

Place tests alongside source code:

```
packages/core/src/test/java/org/printflow/lite/core/service/UserServiceTest.java
```

### Integration Tests

Integration tests may require:

- Running PostgreSQL instance
- CUPS service
- Configured test data

Use Maven profiles to skip integration tests by default:

```bash
# Skip integration tests
mvn test -DskipITs

# Run integration tests
mvn verify -Pit
```

### Test Naming

```java
@Test
void getUserById_existingUser_returnsUser() { }

@Test
void getUserById_nonExistingUser_returnsNull() { }

@Test
void getUserById_nullId_throwsException() { }
```

---

## Creating a New Package

To add a new Maven module to the monorepo:

1. Create the directory structure:
```bash
mkdir -p packages/new-package/src/main/java/org/printflow/lite/newpackage
mkdir -p packages/new-package/src/main/resources
mkdir -p packages/new-package/src/test/java/org/printflow/lite/newpackage
```

2. Create `pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.printflow.lite</groupId>
        <artifactId>printflowlite</artifactId>
        <version>1.7.0</version>
    </parent>

    <artifactId>printflowlite-new-package</artifactId>
    <packaging>jar</packaging>
    <name>PrintFlowLite New Package</name>

    <dependencies>
        <dependency>
            <groupId>org.printflow.lite</groupId>
            <artifactId>printflowlite-common</artifactId>
            <version>1.7.0</version>
        </dependency>
    </dependencies>
</project>
```

3. Add to root `pom.xml`:
```xml
<modules>
    <!-- existing modules -->
    <module>packages/new-package</module>
</modules>
```

---

## Submitting Changes

### Branch Strategy

- `main` — Stable release branch
- `develop` — Integration branch (if applicable)
- `feature/<name>` — Feature development
- `fix/<issue>` — Bug fixes

### Pull Request Process

1. **Fork** the repository
2. **Create a branch** from `develop` (or `main` if no develop branch):
   ```bash
   git checkout -b feature/my-new-feature
   ```
3. **Make your changes** — follow code standards
4. **Write tests** — maintain or improve test coverage
5. **Run the build**:
   ```bash
   mvn clean install -DskipTests
   ```
6. **Commit** with a clear message:
   ```bash
   git commit -m "Add feature: user balance alerts via email"
   ```
7. **Push** to your fork:
   ```bash
   git push origin feature/my-new-feature
   ```
8. **Open a Pull Request** on GitHub

### Commit Message Format

```
<type>(<scope>): <short description>

<longer description if needed>

Fixes #<issue-number>
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

Examples:
```
feat(core): add user balance threshold alerts
fix(server): resolve null pointer in print job queue
docs(admin): update printer configuration section
```

---

## Debugging

### Remote Debugging

Enable remote debugging when starting the server:

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar target/printflowlite-server-1.7.0.war
```

Connect from IDE with:
- Host: `localhost`
- Port: `5005`

### Logging

Increase log level for specific packages:

```properties
# In log4j.properties
log4j.logger.org.printflow.lite.server.web=DEBUG
log4j.logger.org.printflow.lite.core.service=DEBUG
```

### Database Debugging

Enable SQL logging in Hibernate:

```properties
# In server.properties
hibernate.show_sql=true
hibernate.format_sql=true
```

---

## CI/CD

### GitHub Actions

Example workflow (`.github/workflows/build.yml`):

```yaml
name: Build and Test

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build
        run: mvn clean install -DskipTests
      - name: Test
        run: mvn test
```

### Docker Build

```dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY target/printflowlite-server-1.7.0.war /opt/printflowlite/
WORKDIR /opt/printflowlite
EXPOSE 8632
CMD ["java", "-jar", "printflowlite-server-1.7.0.war"]
```

---

## Additional Resources

- [SavaPage Community Wiki](https://wiki.savapage.org/) — Original project wiki
- [Apache Wicket Documentation](https://wicket.apache.org/learn/) — Web framework docs
- [Jetty Documentation](https://www.eclipse.org/jetty/documentation/) — Embedded server
- [CometD Documentation](https://docs.cometd.org/) — WebSocket push
- [JPA / Hibernate Docs](https://hibernate.org/orm/documentation/) — Persistence
