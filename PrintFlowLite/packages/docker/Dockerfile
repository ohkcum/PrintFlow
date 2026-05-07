# =============================================================================
# PrintFlowLite Dockerfile
# =============================================================================
# Builds a Docker image for PrintFlowLite with all required dependencies.
#
# Usage:
#   1. Place your PrintFlowLite installer in this directory:
#      mv printflowlite-setup-*.bin printflowlite-setup.bin
#   2. Build: docker build -t printflowlite/server:latest .
#   3. Run: docker compose up -d
# =============================================================================

FROM ubuntu:noble

# PrintFlowLite version (update when new versions are released)
ENV PFL_VERSION=1.7.0

# Namespace prefix for environment variables
ENV PFL_NS=PFL_

# Required for Docker container detection
ENV PFL_CONTAINER=DOCKER

# Avoid interactive prompts during apt install
ENV DEBIAN_FRONTEND=noninteractive

# -----------------------------------------------------------------------------
# Install system dependencies
# -----------------------------------------------------------------------------
RUN apt-get update && apt-get install -y --no-install-recommends \
    # Core print dependencies
    cups \
    cups-bsd \
    printer-driver-escpr \
    # PDF processing
    poppler-utils \
    qpdf \
    imagemagick \
    wkhtmltopdf \
    librsvg2-bin \
    libheif-examples \
    # Networking / discovery
    avahi-daemon \
    avahi-discover \
    libnss-mdns \
    # Java runtime
    default-jdk \
    # Utilities
    wget \
    gnupg \
    curl \
    binutils \
    cpio \
    vim-tiny \
    findutils \
    iputils-ping \
    hplip \
    # Process supervisor
    supervisor \
    # Cleanup
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /tmp/* /var/tmp/*

# -----------------------------------------------------------------------------
# Create printflowlite user and group
# -----------------------------------------------------------------------------
RUN useradd -rm \
    -d /opt/printflowlite \
    -s /bin/bash \
    -g lpadmin \
    printflowlite \
    && mkdir -p /opt/printflowlite \
    && chown -R printflowlite:printflowlite /opt/printflowlite

# -----------------------------------------------------------------------------
# Set CUPS admin password
# -----------------------------------------------------------------------------
RUN echo 'printflowlite:change-me' | chpasswd

# -----------------------------------------------------------------------------
# Install PrintFlowLite from installer
# Note: The installer is expected to be present in the Docker build context.
# Place printflowlite-setup.bin in the same directory as Dockerfile.
# -----------------------------------------------------------------------------
WORKDIR /opt/printflowlite

COPY printflowlite-setup.bin ./printflowlite-setup.bin

RUN chmod +x ./printflowlite-setup.bin \
    && su printflowlite -c "./printflowlite-setup.bin -n" \
    && /opt/printflowlite/server/bin/linux-x64/roottasks pam \
    && rm ./printflowlite-setup.bin

# -----------------------------------------------------------------------------
# Prepare log directories with correct permissions
# -----------------------------------------------------------------------------
RUN mkdir -p /opt/printflowlite/server/logs \
    /opt/printflowlite/server/data \
    /opt/printflowlite/server/custom \
    /opt/printflowlite/server/ext \
    && chown -R printflowlite:printflowlite /opt/printflowlite/server/logs \
    && chown -R printflowlite:printflowlite /opt/printflowlite/server/data

# -----------------------------------------------------------------------------
# Volumes
# -----------------------------------------------------------------------------
VOLUME ["/etc/cups", "/opt/printflowlite"]

# -----------------------------------------------------------------------------
# Expose ports
# -----------------------------------------------------------------------------
# 631   - CUPS HTTP
# 8631  - PrintFlowLite HTTP (disabled in container)
# 8632  - PrintFlowLite HTTPS
EXPOSE 631 8631 8632

# -----------------------------------------------------------------------------
# Supervisor configuration
# -----------------------------------------------------------------------------
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# -----------------------------------------------------------------------------
# Health check
# -----------------------------------------------------------------------------
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -k -f https://localhost:8632/ || exit 1

# -----------------------------------------------------------------------------
# Start command
# -----------------------------------------------------------------------------
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
