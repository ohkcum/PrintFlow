# PrintFlowLite

> Open Print Portal for Secure Printing, Pay-Per-Print, Delegated Print, Job Ticketing, Auditing and PDF Creation.

PrintFlowLite is a fork of [SavaPage](https://www.savapage.org/) — an open-source print management system — being developed further under a new name and architecture.

## Overview

PrintFlowLite is a Java EE web application built with Apache Wicket and Jetty. It provides a comprehensive print portal with secure printing, pay-per-print, delegated printing, job ticketing, and auditing capabilities.

## Tech Stack

- **Backend**: Java EE (JDK 11+)
- **Persistence**: JPA / Hibernate
- **Database**: Apache Derby (embedded dev) or PostgreSQL (production)
- **Web Server**: Jetty (embedded)
- **Web Framework**: Apache Wicket + jQuery Mobile (adaptive HTML5)
- **PDF Manipulation**: iText, PDFBox, JasperReports
- **Build Tool**: Apache Maven
- **Real-time**: CometD WebSocket
- **CI/CD**: GitHub Actions

## Architecture

PrintFlowLite is organized as a Maven-based monorepo with the following packages:

| Package | Description |
|---------|-------------|
| `packages/server` | Web Server — Jetty-based web application (Wicket, REST, CometD) |
| `packages/core` | Core Library — Business logic and JPA persistence |
| `packages/common` | Common Library — Shared code for client and server |
| `packages/client` | Client Application — Java Swing desktop client |
| `packages/ext` | Extension Interface — Plugin API for extensions |
| `packages/cups-notifier` | CUPS Notifier — C/C++ CUPS integration binary |
| `packages/ppd` | PostScript Printer Driver — PPD files and Windows setup |
| `packages/docker` | Docker — Community Docker deployment setup |
| `packages/android-print` | Android Print — Reference for Android Print Service app |

## Getting Started

### Prerequisites

- JDK 11 or higher
- Maven 3.6+
- PostgreSQL (optional, for production) or use embedded Derby

### Build

Build all packages from the monorepo root:

```bash
mvn clean install
```

Or build individual packages:

```bash
cd packages/server
mvn clean package -DskipTests
```

### Quick Start

See the [Getting Started](docs/getting-started.md) guide for detailed installation instructions.

## Features

- Internet Printing Protocol (IPP) and JetDirect support
- AirPrint and ChromeOS printing
- Android Print Service (separate Android app)
- Mail printing via IMAP
- Web Print (file upload)
- File upload for PDF creation
- Secure pull printing with NFC card authentication
- Pay-per-print with cost tracking per user/group
- Delegated printing (print proxy)
- Job ticketing with finishing options
- Complete print auditing and JasperReports
- User management (Local, LDAP/AD, OAuth)
- 2FA support (TOTP, YubiKey, Telegram)
- PDF processing (repair, optimize, encrypt, verify)
- Multi-language support (i18n)
- REST API (`/restful/v1`) and JSON API (`/api`)
- CometD real-time push notifications

## Deployment Options

### Traditional

Install as a systemd service on GNU/Linux. See [Getting Started](docs/getting-started.md).

### Docker

Community Docker setup available in `packages/docker/`:

```bash
cd packages/docker
docker build -t printflowlite/server:latest .
docker compose up -d
```

### CI/CD

GitHub Actions workflows are available in `.github/workflows/`:

- `build.yml` — Build, test, and validate on push/PR
- `release.yml` — Create GitHub releases with artifacts

## Documentation

Comprehensive documentation is available in the [docs/](docs/index.md) directory:

- [Getting Started](docs/getting-started.md) — Installation, build, and quick start
- [Architecture](docs/architecture.md) — System architecture and components
- [Features Reference](docs/features.md) — Complete list of features
- [User Guide](docs/user-guide.md) — End-user documentation
- [Admin Guide](docs/admin-guide.md) — Server administration guide
- [API Reference](docs/api-reference.md) — REST API, JSON API, and WebSocket API
- [Security](docs/security.md) — Security best practices
- [Troubleshooting](docs/troubleshooting.md) — Common issues and solutions
- [Development Guide](docs/development.md) — Building, testing, and contributing
- [Roadmap](docs/roadmap.md) — Project goals and roadmap

## License

Copyright (c) 2010-2025 Datraverse B.V. — Licensed under GNU Affero General Public License (AGPL) v3.

PrintFlowLite is a derivative work. See LICENSE files in each package for details.

## Project Structure

```
PrintFlowLite/
├── .github/
│   └── workflows/       # GitHub Actions CI/CD pipelines
├── docs/                 # Documentation
├── packages/
│   ├── server/          # Web Server (Jetty + Wicket)
│   ├── core/            # Core Library (business logic + JPA)
│   ├── common/          # Common Library
│   ├── client/          # Desktop Client Application (Swing)
│   ├── ext/             # Extension Interface (plugin API)
│   ├── cups-notifier/   # CUPS Notifier (C/C++)
│   ├── ppd/             # PostScript Printer Driver (PPD)
│   ├── docker/           # Community Docker deployment
│   └── android-print/    # Android Print Service reference
├── tmp/                  # Temporary upstream git bare repos
└── README.md
```
