# PrintFlowLite — Project Config

## Project Overview

**PrintFlowLite** is an open-source print management system (fork of SavaPage). It is a Java EE web application built with Apache Wicket and Jetty, providing secure pull printing, pay-per-print, job ticketing, auditing, and PDF creation.

**License:** AGPL-3.0-or-later
**Source Compatibility:** Java 8 | **Runtime:** JDK 11+

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 8 source, JDK 11+ runtime |
| Build Tool | Apache Maven |
| Web Server | Jetty 10 (embedded) |
| Web Framework | Apache Wicket 9.22 |
| Persistence | JPA / Hibernate |
| Database | Apache Derby (embedded dev) or PostgreSQL (production) |
| REST API | Jersey 2.45 (`/restful/v1`) |
| Real-time | CometD 6 (WebSocket push) |
| PDF | iText, PDFBox, JasperReports |
| Frontend | jQuery Mobile (adaptive HTML5) |
| Testing | JUnit Jupiter 5.5 |
| Logging | SLF4J |

---

## Package Structure

```
PrintFlowLite/
├── packages/
│   ├── server/          # Web Server — Jetty + Wicket + REST + CometD (WAR)
│   ├── core/            # Core Library — business logic + JPA entities + services
│   ├── common/          # Common Library — shared code client/server
│   ├── client/          # Desktop Client — Java Swing application
│   ├── ext/             # Extension Interface — plugin API for extensions
│   ├── cups-notifier/   # CUPS Notifier — C/C++ binary
│   ├── ppd/             # PostScript Printer Driver files
│   └── docker/          # Docker deployment
```

---

## Critical Patterns & Conventions

### JPA Entities
- All entities extend `org.printflow.lite.core.jpa.Entity`.
- Entities use JPA annotations directly on fields (not via XML).
- Table names are defined as `public static final String TABLE_NAME = "tbl_..."`.
- Use `@TableGenerator` with `allocationSize = 1` for ID generation.
- LAZY fetch is the default for collections.

### Exception Handling
- Base exception class: `org.printflow.lite.core.SpException extends RuntimeException`.
- `SpException` has an `isWarning()` method — return `true` for non-critical errors.
- Never silently catch and ignore `SpException` or its subclasses.
- For REST endpoints, always map exceptions to appropriate HTTP status codes.

### REST Services
- REST services live in `packages/server/src/main/java/.../restful/services/`.
- All REST DTOs extend `org.printflow.lite.server.restful.dto.AbstractRestDto`.
- All REST services implement `IRestService`.
- Extend `AbstractRestService` for shared REST functionality.
- Error responses must follow the existing `RestResponseDto` structure.

### Package Naming
- Base package: `org.printflow.lite`
- Server: `org.printflow.lite.server`
- Core: `org.printflow.lite.core`
- Common: `org.printflow.lite.common`

### Build & Test
- Build from monorepo root: `mvn clean install`
- Build server only: `cd packages/server && mvn clean package -DskipTests`
- Run tests: `mvn test` (or per-package)
- The `maven-surefire-plugin` uses `useSystemClassLoader=false` — do not change this.

### Logging
- Use SLF4J (`org.slf4j.Logger`) — never `System.out.println`.
- Log at appropriate levels: ERROR for failures, WARN for degraded state, INFO for significant events, DEBUG for detailed flow.
- Never log sensitive data (passwords, tokens, user content).

### Code Style
- License header required on every file (SPDX format — see existing files).
- All source files: UTF-8 encoding.
- Column ruler at 120 characters.
- Tab size: 4 spaces.

### Web Pages (Wicket)
- Wicket pages live in `packages/server/src/main/java/.../pages/` and `packages/server/src/main/java/.../webapp/`.
- WebApp classes (`WebApp*.java`) extend `AbstractWebAppPage`.
- Page-specific messages are defined in `.properties` files (i18n).
