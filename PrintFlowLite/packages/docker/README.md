# PrintFlowLite Docker

Community-contributed Docker setup for running PrintFlowLite in containers.

> **Note**: This Docker setup is community-contributed, originally based on the [SavaPage Docker setup](https://github.com/jboillot/savapage) by Jérôme Boillot, adapted for PrintFlowLite. It is not officially supported by the PrintFlowLite project.

## Overview

This package provides Docker and Docker Compose configuration for running PrintFlowLite in isolated containers, with:

- PrintFlowLite server with embedded Jetty
- PostgreSQL database for production use
- CUPS print server
- Persistent volumes for data, logs, and configuration
- Supervisord for managing multiple processes

## Prerequisites

- Docker Engine 20.10+
- Docker Compose v2

### Install on Debian/Ubuntu

```bash
sudo apt install docker.io docker-compose-v2
sudo usermod -aG docker $USER
# Log out and back in for group change to take effect
```

## Quick Start

### 1. Create project directory

```bash
mkdir -p ~/Docker-containers/printflowlite
cd ~/Docker-containers/printflowlite
```

### 2. Copy Docker files

Copy the contents of this directory to your project folder.

### 3. Download PrintFlowLite installer

Download the PrintFlowLite installer binary to the project directory:

```bash
# Rename the downloaded installer to match the Dockerfile expectation
mv printflowlite-setup-*.bin printflowlite-setup.bin
```

### 4. Build the image

```bash
docker build -t printflowlite .
```

### 5. Start containers

```bash
docker compose up -d
```

### 6. Access PrintFlowLite

| Service | URL |
|---------|-----|
| Admin Web App | https://localhost:8643/ |
| User Web App | https://localhost:8643/user |
| CUPS Web Interface | http://localhost:6631/printers/ |

Default admin credentials: `admin` / `changeme`

---

## File Structure

```
docker/
├── docker-compose.yml       # Container orchestration
├── Dockerfile              # Image definition
├── supervisord.conf        # Process supervisor config
├── docker.env              # Environment variables
├── .dockerignore          # Docker build exclusions
└── README.md              # This file
```

---

## Configuration

### Environment Variables (docker.env)

Key settings for the PrintFlowLite server inside the container:

```properties
# Namespace prefix (do not change)
PFL_NS=PFL_

# Required for Docker container detection
PFL_CONTAINER=DOCKER

# Visitor organization name
PFL_SRV_01=visitor.organization:My Organization

# Server ports
# PFL_SRV_02=server.port:8631      # HTTP (disabled in container)
# PFL_SRV_03=server.ssl.port:8632  # HTTPS external

# PostgreSQL database
PFL_SRV_11=database.type:PostgreSQL
PFL_SRV_12=database.driver:org.postgresql.Driver
PFL_SRV_13=database.url:jdbc:postgresql://printflowlite-postgres:5432/printflowlite
PFL_SRV_14=database.user:printflowlite
PFL_SRV_15=database.password:change-me
```

### Volumes

| Volume | Container Path | Description |
|--------|---------------|-------------|
| `printflowlite_custom` | `/opt/printflowlite/server/custom` | Custom configuration |
| `printflowlite_data` | `/opt/printflowlite/server/data` | SafePages, letters, archive |
| `printflowlite_ext` | `/opt/printflowlite/server/ext` | Extensions/plugins |
| `printflowlite_logs` | `/opt/printflowlite/server/logs` | Application logs |
| `printflowlite_cups` | `/etc/cups` | CUPS printer configuration |
| `printflowlite_database` | `/var/lib/postgresql/data` | PostgreSQL data |

### Ports

| Container Port | Host Port | Protocol | Description |
|---------------|----------|---------|-------------|
| 631 | 6631 | HTTP | CUPS web interface (local only) |
| 8631 | — | HTTP | PrintFlowLite HTTP (disabled) |
| 8632 | 8643 | HTTPS | PrintFlowLite HTTPS |
| 5432 | 5443 | TCP | PostgreSQL (local only) |

> Port 8632 (external SSL) is mapped to host 8643 to avoid conflicts with any existing PrintFlowLite installation.

---

## Common Tasks

### View running containers

```bash
docker compose ps
```

### View logs

```bash
# PrintFlowLite logs
docker compose logs -f printflowlite

# PostgreSQL logs
docker compose logs -f postgres

# All logs
docker compose logs -f
```

### Open shell in PrintFlowLite container

```bash
docker compose exec printflowlite bash
```

### Open shell in PostgreSQL container

```bash
docker compose exec postgres bash
```

### Stop containers

```bash
docker compose down
```

### Stop and remove volumes (WARNING: deletes all data)

```bash
docker compose down -v
```

### Rebuild after changes

```bash
docker compose build --no-cache printflowlite
docker compose up -d
```

---

## Adding Printers

### Via CUPS Web Interface

```bash
# Open shell in container
docker compose exec printflowlite bash

# Enable remote CUPS access
cupsctl --remote-any

# Exit shell
exit

# Access CUPS at http://localhost:6631
```

Default CUPS admin user: `printflowlite`
Default CUPS password: set in Dockerfile via `chpasswd`

### Via Command Line

```bash
docker compose exec printflowlite bash

# Add a network printer
lpadmin -p MyPrinter -v ipp://192.168.1.100/ipp/print -E

# Set as default
lpoptions -d MyPrinter

exit
```

---

## Security Notes

### Default Passwords

Change these default passwords before production use:

1. **PrintFlowLite admin**: Change via Admin Web App > User Details
2. **CUPS admin**: Set in Dockerfile before building:
   ```dockerfile
   RUN echo 'printflowlite:your-secret-password' | chpasswd
   ```
3. **PostgreSQL**: Set in `docker.env`

### Network Access

This setup exposes ports as follows:

- **CUPS (6631)**: localhost only — safe for local development
- **PrintFlowLite HTTPS (8643)**: all interfaces — accessible from network
- **PostgreSQL (5443)**: localhost only — safe

For production, consider:

- Using a reverse proxy (nginx, traefik) with TLS termination
- Restricting port access with firewall rules
- Using Docker networks for container isolation

---

## Troubleshooting

### Container exits immediately

Check logs:
```bash
docker compose logs printflowlite
```

Common causes:
- Port 8631 already in use — HTTP is disabled, check `server.port=0`
- Database connection failed — verify PostgreSQL is running
- Installer not found — ensure `printflowlite-setup.bin` is in the build context

### Can't access PrintFlowLite from browser

1. Check if container is running: `docker compose ps`
2. Check if port is exposed: `docker port printflowlite`
3. Check logs for startup errors: `docker compose logs printflowlite`
4. Try accessing via curl:
   ```bash
   curl -k https://localhost:8643/
   ```

### Printers not visible

1. Access CUPS: http://localhost:6631
2. Check printer status in Admin Web App
3. Verify CUPS is running: `docker compose exec printflowlite pgrep cupsd`

### Database connection errors

1. Verify PostgreSQL is running: `docker compose ps postgres`
2. Check database logs: `docker compose logs postgres`
3. Test connection: `docker compose exec printflowlite bash -c "psql -h postgres -U printflowlite -d printflowlite -c 'SELECT 1'"`
4. Check connection string in `docker.env`

---

## Building a Custom Image

### Pre-built vs. Build from Source

This Dockerfile downloads a binary installer. To build from source:

1. Build PrintFlowLite from source:
   ```bash
   cd PrintFlowLite
   mvn clean package -DskipTests
   ```
2. Copy the WAR file into the Docker context
3. Modify the Dockerfile to use the WAR instead of the installer

Example modification:
```dockerfile
COPY printflowlite-server-1.7.0.war /tmp/printflowlite.war
# Skip installer download, deploy WAR directly
```

---

## License

This Docker setup is community-contributed and licensed under MIT. PrintFlowLite itself is licensed under GNU AGPL v3.

## References

- [SavaPage Docker Guide](https://wiki.savapage.org/doku.php?id=howto:docker) — Original SavaPage Docker documentation
- [Jérôme Boillot's SavaPage Docker](https://github.com/jboillot/savapage) — Community Docker implementation this is based on
- [PrintFlowLite Documentation](../docs/)
