# Getting Started with PrintFlowLite

This guide walks you through installing and running PrintFlowLite.

## System Requirements

### Server

PrintFlowLite runs on modern GNU/Linux systems (Debian, RHEL, openSUSE) with systemd.

#### Hardware

- **CPU**: 2 cores minimum (4+ recommended)
- **RAM**: 2 GB minimum (4+ GB recommended)
- **Disk**: 1 GB minimum, plus extra space per user for SafePages storage
- **Network**: Ethernet connection

#### Software Dependencies

| Dependency | Version | Purpose | Install (Debian) |
|---|---|---|---|
| Java JDK | 11+ | Runtime environment | `sudo apt install default-jdk-headless` |
| Maven | 3.6+ | Build tool | `sudo apt install maven` |
| CUPS | 1.4+ | Local printer queues for proxy printing | `sudo apt install cups cups-bsd` |
| PostgreSQL | 9.6+ | Production database (optional) | `sudo apt install postgresql` |
| Poppler | — | PDF to image conversion | `sudo apt install poppler-utils` |
| QPDF | — | PDF decryption for web/mail print | `sudo apt install qpdf` |
| ImageMagick | — | Image manipulation | `sudo apt install imagemagick` |
| wkhtmltopdf | — | HTML to PDF rendering (optional) | `sudo apt install wkhtmltopdf` |
| rsvg-convert | — | SVG to PNG for drawing | `sudo apt install librsvg2-bin` |
| Avahi | — | IPP Everywhere / AirPrint discovery | `sudo apt install avahi-utils avahi-discover libnss-mdns` |

> **Note for Production**: The embedded Apache Derby database is suitable for evaluation. For multi-user production environments, use PostgreSQL to avoid locking, deadlock, and out-of-memory errors.

### Clients

- **Browser**: HTML5-compatible browser (Chrome, Firefox, Safari, Edge)
- **Print Methods**: IPP, JetDirect (TCP port 9100), or AirPrint
- **Mobile**: Any browser on iOS or Android

---

## Building from Source

### Prerequisites

- JDK 11 or higher
- Maven 3.6+
- Git

### Clone and Build

```bash
# Clone the repository
git clone https://github.com/YOUR_GITHUB/PrintFlowLite.git
cd PrintFlowLite

# Build all packages (skip tests for faster build)
mvn clean install -DskipTests
```

### Build Individual Packages

PrintFlowLite is organized as a Maven monorepo with build order:

```bash
# 1. Common library first
cd packages/common
mvn clean install -DskipTests

# 2. Core business logic
cd ../core
mvn clean install -DskipTests

# 3. Extension interface
cd ../ext
mvn clean install -DskipTests

# 4. Web server
cd ../server
mvn clean package -DskipTests

# 5. Desktop client
cd ../client
mvn clean package -DskipTests
```

> **Note**: The `cups-notifier` and `ppd` packages require C/C++ build tools and CUPS development headers. See their respective READMEs.

### Output Artifacts

After a successful build:

| Package | Artifact | Location |
|---|---|---|
| `server` | WAR file | `packages/server/target/printflowlite-server-1.7.0.war` |
| `client` | Executable JAR | `packages/client/target/app/bin/printflowlite-client` |
| `cups-notifier` | Binary | `cups-notifier/printflowlite-notifier` |
| `ppd` | PPD file | `ppd/PRINTFLOWLITE.ppd` |

---

## Running the Server

### Option 1: Embedded Jetty

The server JAR includes an embedded Jetty runner:

```bash
cd packages/server
java -jar target/printflowlite-server-1.7.0.war
```

### Option 2: Deploy to External Servlet Container

Deploy the WAR file to Jetty, Tomcat, or any Servlet 4.0 container:

```bash
# Example: copy to Jetty webapps directory
cp packages/server/target/printflowlite-server-1.7.0.war \
   /opt/jetty/webapps/printflowlite.war
```

### Option 3: Docker

See [`packages/docker/README.md`](../packages/docker/) for the full Docker setup guide.

Quick start:

```bash
cd packages/docker

# 1. Download PrintFlowLite installer and save as printflowlite-setup.bin
#    (or use the community image jboillot/savapage as a base)

# 2. Build the image
docker build -t printflowlite/server:latest .

# 3. Start containers
docker compose up -d

# 4. Access PrintFlowLite
#    Admin Web App: https://localhost:8643/admin
#    User Web App:  https://localhost:8643/user
```

> **Note**: Docker support is community-contributed. See [packages/docker/](../packages/docker/) for details including docker-compose, Dockerfile, and supervisor configuration.

---

## Initial Configuration

After first startup, access the Admin Web App at:

```
https://your-server:8632/
```

### Default Admin Login

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `changeme` |

> **Important**: Change the default admin password immediately after first login.

### Post-Installation Checklist

1. **Change admin password** — Admin Web App > User Details
2. **Configure user directory** — Options > User Source (LDAP/AD integration)
3. **Add proxy printers** — Options > Proxy Printers (CUPS queues)
4. **Configure SMTP** — Options > Mail (for Mail Print notifications)
5. **Set up printing queues** — Users print to the PrintFlowLite IPP queue

---

## Upgrading from Previous Versions

See the [SavaPage migration guide](https://www.savapage.org/docs/manual/app-upgrading.html) for detailed upgrade instructions when migrating from SavaPage to PrintFlowLite.

---

## Next Steps

- Read the [Architecture Overview](architecture.md) to understand how components interact
- Explore the [User Guide](user-guide.md) for end-user workflows
- Configure the [Admin Guide](admin-guide.md) for your organization
