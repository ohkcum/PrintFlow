# PrintFlowLite Security

This document covers security best practices, authentication mechanisms, and data protection in PrintFlowLite.

---

## Authentication

### Authentication Methods

PrintFlowLite supports multiple authentication methods that can be configured in **Admin Web App > Options > User Authentication**.

| Method | Security Level | Use Case |
|---|---|---|
| **Local** | Medium | Standalone deployment, testing |
| **LDAP / Active Directory** | High | Corporate networks |
| **OAuth 2.0** | High | Cloud identity providers |
| **NFC Card** | High | Physical access control |
| **API Key** | Medium | Programmatic access |

### LDAP / Active Directory

LDAP authentication integrates with your corporate identity infrastructure:

- Passwords are verified against the LDAP server (not stored locally)
- Users must exist in both LDAP and PrintFlowLite (or auto-created)
- Group membership can be synced for role assignment
- Use LDAPS (`ldaps://`) for encrypted communication
- Certificate validation should be enabled in production

**Security Recommendations:**
- Use LDAPS with a valid TLS certificate
- Configure a dedicated service account for binding
- Limit bind DN permissions to read-only user queries
- Enable LDAP signing and LDAP channel binding where supported

### OAuth 2.0

OAuth providers (Google, GitHub, custom) handle password management externally:

- Users authenticate with the OAuth provider
- PrintFlowLite receives an access token and user info
- No passwords are stored in PrintFlowLite for OAuth users
- Authorization can be restricted to specific email domains

**Supported Providers:**
- Google Workspace / Google accounts
- GitHub
- Custom OAuth 2.0 server

### API Key Authentication

For programmatic access via the REST API:

- API keys should be treated as passwords
- Rotate keys periodically
- Use different keys per application
- Restrict API key permissions to minimum required scope

---

## Authorization

### Role-Based Access Control

| Role | Description | Capabilities |
|---|---|---|
| **USER** | Standard end user | Print, manage own SafePages, view own transactions |
| **MANAGER** | Group manager | View/manage group members' print jobs, group reports |
| **ADMIN** | System administrator | Full system configuration, all user management |

### Proxy Printer Access Control

Administrators can restrict proxy printer access:

- **Public** — All users can print
- **Group-restricted** — Only specified groups can print
- **User-restricted** — Only specified users can print
- **Hidden** — Printer not shown in Web App but accessible via API

---

## Secure Printing

### Pull Print (Secure Release)

By default, print jobs are held in SafePages and not sent to physical printers until the user releases them. This prevents sensitive documents from sitting unattended at the printer.

**Secure release can be combined with:**
- Web login authentication
- NFC card authentication at a terminal
- Delegation authentication

### NFC Card Authentication

For physical card-based authentication:

1. User taps NFC card on a card reader connected to a terminal
2. PrintFlowLite verifies the card against registered cards
3. User can then release jobs or authenticate sessions

**Security Considerations:**
- Cards should use secure authentication (MIFARE DESFire, etc.)
- Card data should be encrypted
- Lost/stolen cards can be quickly deactivated

### Delegation (Print on Behalf)

Users can print on behalf of others when granted delegation rights:

- The delegator explicitly grants delegation rights to specific users
- All delegated print jobs are attributed to the delegated user
- Full audit trail is maintained

---

## Network Security

### TLS / HTTPS

PrintFlowLite uses Jetty as its embedded server. Configure HTTPS in the Jetty configuration:

**Generate a keystore:**

```bash
keytool -genkey -alias printflowlite -keyalg RSA \
  -keystore /etc/printflowlite/keystore.jks \
  -storepass your-store-password \
  -keypass your-key-password \
  -dname "CN=printflowlite.example.com, O=Your Org"
```

**Enable HTTPS in jetty-config (recommended production):**

```properties
# server.properties
ssl.enabled=Y
ssl.port=8632
ssl.keystore=/etc/printflowlite/keystore.jks
ssl.keystore.password=your-store-password
ssl.keymanager.password=your-key-password
ssl.keymanager.algorithm=SunX509
ssl.protocol=TLS
ssl.include.cipher.suites=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
```

> **Important**: Use a trusted CA-signed certificate in production. Self-signed certificates are suitable for testing only.

### HTTP Strict Transport Security (HSTS)

Enable HSTS to force browsers to use HTTPS:

```properties
# In jetty-config or server.properties
hsts.enabled=Y
hsts.max.age=31536000
```

### Secure Cookie Settings

Configure secure session cookies:

```properties
session.cookie.secure=true
session.cookie.httpOnly=true
session.cookie.sameSite=Strict
```

### Firewall Configuration

Restrict access to the PrintFlowLite server:

```bash
# Allow only necessary ports
# HTTPS (web interface)
-A INPUT -p tcp --dport 8632 -s 10.0.0.0/8 -j ACCEPT

# IPP (printing)
-A INPUT -p tcp --dport 631 -s 10.0.0.0/8 -j ACCEPT

# JetDirect (printing)
-A INPUT -p tcp --dport 9100 -s 10.0.0.0/8 -j ACCEPT

# CUPS admin (localhost only)
-A INPUT -p tcp --dport 631 -s 127.0.0.1 -j ACCEPT
```

### CUPS Access Control

Restrict CUPS access to the local network:

```
# /etc/cups/cupsd.conf
Listen localhost:631
Port 631

# Restrict browsing to local network
BrowseLocalProtocols dnssd

# Require authentication for admin operations
<Location /admin>
  Order allow,deny
  Allow from 127.0.0.1
  Allow from 10.0.0.0/8
  Require valid-user
</Location>
```

---

## Data Protection

### Database Security

#### PostgreSQL (Production)

- Enable PostgreSQL authentication (`scram-sha-256` or `cert`)
- Use a dedicated PostgreSQL user with minimum required permissions
- Enable SSL connections to PostgreSQL
- Regularly back up the database
- Use `pg_hba.conf` to restrict access

```bash
# /etc/postgresql/15/main/pg_hba.conf
# IPv4 local connections:
hostssl all all 127.0.0.1/32 scram-sha-256
hostssl all all 10.0.0.0/8 scram-sha-256
```

#### Embedded Derby (Development)

The embedded Derby database is suitable for evaluation only:
- No encryption at rest
- No network access control
- Not suitable for multi-user production environments

### Data at Rest Encryption

For environments requiring data-at-rest encryption:

- Use full-disk encryption (LUKS, BitLocker)
- For PostgreSQL, use `pg_encryption` or tablespace encryption
- Encrypt backup files

### GDPR Compliance

PrintFlowLite supports GDPR compliance through:

- **Right to access** — Export user data via Admin Web App or API
- **Right to erasure** — Delete user accounts and associated data
- **Data minimization** — Configure retention policies
- **Audit logging** — Complete activity records

### Data Retention

Configure retention policies:

| Data Type | Default Retention | Configurable |
|---|---|---|
| SafePages | 7 days | Yes |
| Audit Log | Indefinite | Configurable |
| Transactions | Indefinite | Configurable |
| User Files | Per SafePages setting | Yes |

---

## Audit Logging

PrintFlowLite maintains comprehensive audit logs covering:

- User authentication (login, logout, failures)
- Print job submission
- Print job release and completion
- SafePages creation and deletion
- User management actions
- Configuration changes
- API access

**Log Location:** `/var/log/printflowlite/printflowlite-audit.log`

**Log Format:**
```
2025-03-01T14:22:00Z AUDIT user=jsmith action=PRINT jobId=123 \
  document="report-q4.pdf" pages=12 cost=0.60 \
  printer="Floor-2-MFD" ip=192.168.1.50
```

---

## Password Policy

Configure password requirements in **Options > User Authentication**:

| Setting | Description |
|---|---|
| **Minimum Length** | Minimum characters (default: 8) |
| **Complexity** | Require uppercase, lowercase, numbers, symbols |
| **Expiry** | Force password change after N days |
| **History** | Prevent reuse of last N passwords |
| **Lockout** | Lock account after N failed attempts |

---

## API Security

### REST API Authentication

- Use HTTPS for all API calls
- Use API keys or OAuth tokens instead of user passwords where possible
- Rotate API keys regularly
- Restrict API access by IP address where possible

### WebSocket (CometD) Security

- Authenticate during handshake
- Use WSS (WebSocket Secure) in production
- Session tokens expire with the HTTP session

---

## Security Hardening Checklist

Before deploying to production:

- [ ] Change default admin password
- [ ] Enable HTTPS with a valid TLS certificate
- [ ] Configure HSTS
- [ ] Enable secure cookie settings
- [ ] Set up a firewall restricting access to necessary ports
- [ ] Use LDAP/LDAPS instead of local authentication
- [ ] Configure strong password policy
- [ ] Enable account lockout
- [ ] Review and restrict proxy printer access
- [ ] Set up log monitoring and alerting
- [ ] Enable automatic backups
- [ ] Switch from embedded Derby to PostgreSQL
- [ ] Configure data retention policies
- [ ] Review firewall rules
- [ ] Enable CUPS access control
- [ ] Restrict CUPS admin interface to localhost
