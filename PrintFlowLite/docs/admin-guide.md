# PrintFlowLite Admin Guide

This guide covers server administration tasks for PrintFlowLite, including configuration, user management, printer setup, and options.

---

## Accessing the Admin Web App

The Admin Web App is available at:

```
https://your-printflowlite-server:8632/admin
```

Log in with an admin account.

> **Default credentials**: Username `admin`, password `changeme` — change immediately after first login.

---

## Options Panel

The Options panel is the central configuration hub. It is organized into categories:

### User Source

Configure where user accounts are sourced from.

#### Local User Source

Users are managed directly in PrintFlowLite. Create users manually or via CSV import.

#### LDAP / Active Directory

Integrate with your corporate directory:

1. Go to **Options > User Source > LDAP**
2. Enter your LDAP server address (e.g., `ldap://your-dc.domain.local`)
3. Set the base DN (e.g., `dc=domain,dc=local`)
4. Configure bind DN and credentials
5. Set user search filter (e.g., `(objectClass=user)`)
6. Set group search filter
7. Test the connection
8. Save and trigger initial sync

| Setting | Description |
|---|---|
| **Server URL** | `ldap://` or `ldaps://` for TLS |
| **Base DN** | Starting point for user searches |
| **Bind DN** | Service account for queries |
| **Bind Password** | Service account password |
| **User Search Filter** | LDAP filter for user objects |
| **Group Search Filter** | LDAP filter for group objects |
| **Sync Interval** | How often to sync users from LDAP |
| **Auto-Create Users** | Automatically create local accounts on first LDAP login |

### User Authentication

Configure how users authenticate.

| Method | Configuration |
|---|---|
| **Local** | Username/password in PrintFlowLite database |
| **LDAP** | Authenticate against LDAP server |
| **OAuth 2.0** | Configure OAuth providers in Integration > OAuth |
| **NFC Card** | Requires NFC reader hardware and card provisioning |

#### Session Settings

- **Session timeout** — Inactivity timeout before automatic logout
- **Remember me** — Allow "remember me" on login
- **Max sessions per user** — Limit concurrent sessions

### Mail (SMTP/IMAP)

Configure Mail Print functionality.

#### Outgoing Mail (SMTP)

1. Go to **Options > Mail > SMTP**
2. Enter your SMTP server address
3. Set the port (default: 587 for TLS, 465 for SSL)
4. Enable TLS/SSL as required
5. Enter credentials if authentication is required
6. Set the sender address (From: header)

#### Incoming Mail (IMAP)

1. Go to **Options > Mail > IMAP**
2. Enter your IMAP server address
3. Set the port (default: 993 for SSL)
4. Enter credentials
5. Set the inbox polling interval
6. Configure trusted sender domains

#### Mail Print Settings

| Setting | Description |
|---|---|
| **Allowed Senders** | Only accept from whitelisted email addresses |
| **Max Attachment Size** | Maximum size per email attachment |
| **Allowed File Types** | Restrict accepted attachment formats |
| **Auto-Delete Processed Mail** | Remove processed emails from inbox |
| **Personal Mail Print Address** | Each user gets a unique inbox address |

### Web Print

Configure file upload via the web interface.

| Setting | Description |
|---|---|
| **Enabled** | Enable/disable Web Print |
| **Max File Size** | Maximum upload size per file |
| **Allowed Types** | Whitelist of accepted MIME types |
| **Daily Quota** | Maximum uploads per user per day |
| **Auto-Delete After** | Days before unprinted uploads expire |

### Internet Print

Configure public (external network) printing.

| Setting | Description |
|---|---|
| **Enabled** | Enable/disable Internet Print |
| **Private Device URI** | Per-user unique IPP/JetDirect URI |
| **Authentication** | Username + app-key (generated per user) |
| **IP Whitelist** | Restrict to specific IP ranges |

### Proxy Print

Configure proxy printer behavior.

| Setting | Description |
|---|---|
| **CUPS Integration** | Enable automatic CUPS queue detection |
| **Default Printer** | Printer pre-selected in print dialog |
| **Default Options** | Pre-set print options (duplex, paper size) |
| **Fit to Page** | Auto-scale documents to match paper |
| **Job Confirmation** | Require user confirmation before printing |
| **Hold Expiry** | Days before unprinted jobs expire |

### Eco Print

Set default eco-friendly print options.

| Setting | Description |
|---|---|
| **Default Duplex** | Set duplex as default |
| **Default Grayscale** | Convert color to grayscale by default |
| **Eco Mode** | Reduce ink/toner usage |
| **N-up Default** | Pages per sheet (1, 2, or 4) |
| **Graphics Removal** | Strip images in draft mode |

### Financial

Configure pay-per-print cost tracking.

#### Cost Rates

Set per-page costs:

| Item | Default Cost |
|---|---|
| Black & white (single-sided) | $0.05 |
| Black & white (duplex) | $0.04 |
| Color (single-sided) | $0.20 |
| Color (duplex) | $0.15 |

#### Balance Limits

| Setting | Description |
|---|---|
| **Initial Balance** | Starting balance for new users |
| **Credit Limit** | How far negative a balance can go |
| **Low Balance Alert** | Notify user when balance drops below threshold |
| **Auto-Refill** | Automatically add credit (requires payment integration) |

### Integration

Configure external integrations.

#### OAuth 2.0

Add OAuth authentication providers:

1. **Google**
   - Client ID and Client Secret from Google Cloud Console
   - Redirect URI: `https://your-server:8632/oauth2/callback/google`

2. **GitHub**
   - Client ID and Client Secret from GitHub Developer Settings
   - Redirect URI: `https://your-server:8632/oauth2/callback/github`

3. **Custom Provider**
   - Authorization URL
   - Token URL
   - Userinfo URL
   - Client ID and Secret

#### REST API

Enable and configure the REST API:

| Setting | Description |
|---|---|
| **Enabled** | Enable REST API |
| **API Key** | Authentication key for API access |
| **Allowed Endpoints** | Restrict to specific API methods |

### Backups

Configure automatic data backups.

| Setting | Description |
|---|---|
| **Enabled** | Enable automatic backups |
| **Schedule** | Daily/weekly/monthly |
| **Destination** | Local directory or remote (SFTP, etc.) |
| **Retention** | Number of backups to keep |
| **Include Database** | Include database dump |
| **Include Files** | Include uploaded files and SafePages |

### Advanced (Config Editor)

Fine-grained configuration via key-value pairs. This is for advanced users.

Example configurations:

```properties
# PDF rendering
system.cmd.wkhtmltopdf.enable=N

# Database connection pool
db.pool.maxActive=20
db.pool.maxIdle=10

# Session timeout (minutes)
session.timeout=30

# Log level
log4j.rootLogger=INFO
```

---

## User Management

### Creating Users

**Manually:**

1. Go to **Users > Create User**
2. Enter username, full name, email
3. Set initial password or send activation email
4. Assign role (User, Manager, Admin)
5. Assign to groups
6. Save

**CSV Import:**

1. Go to **Users > Import**
2. Upload a CSV file with columns: `username,full_name,email,role,groups`
3. Preview and confirm
4. Import

**Auto-Create (LDAP/OAuth):**

Enable auto-creation in Options > User Source. Users are created automatically on first login.

### Managing Users

- **Edit user** — Change details, role, groups
- **Reset password** — Send password reset email or set temporary password
- **Disable user** — Prevent login without deleting
- **Delete user** — Remove user and optionally their data
- **View activity** — See user's print history and transactions

### Groups

Create groups to organize users and apply shared settings:

1. Go to **Groups > Create Group**
2. Enter group name and description
3. Add members
4. Set group-specific options (e.g., cost rates, printer access)

---

## Proxy Printer Management

### Adding Proxy Printers

Proxy Printers are physical printers exposed through local CUPS queues.

1. Install the printer on the server using CUPS web interface (`http://localhost:631`)
2. PrintFlowLite automatically detects CUPS queues
3. Go to **Printers > Proxy Printers**
4. Confirm the detected printer
5. Set a friendly name and location

### Printer Settings

| Setting | Description |
|---|---|
| **Name** | Display name in Web App |
| **Location** | Physical location (e.g., "Floor 2, MFD") |
| **Description** | Additional description |
| **CUPS Queue** | Name of the underlying CUPS queue |
| **Default Options** | Pre-set print options |
| **Access Control** | Restrict to specific users or groups |
| **Cost Rate** | Override default cost for this printer |
| **SNMP Community** | Community string for status monitoring |
| **DNS-SD Name** | mDNS advertisement name |

### Printer Groups

Group printers by location or type:

1. Go to **Printers > Groups**
2. Create a group (e.g., "Floor 2 Printers")
3. Add printers to the group
4. Users can select the group to see all printers in it

### Monitoring

View printer status:

- **Online/Offline** — Is the printer reachable?
- **Toner Levels** — Approximate ink/toner remaining (via SNMP)
- **Paper Supply** — Paper in trays (via SNMP)
- **Job Queue** — Current jobs on the physical printer
- **Last Activity** — Most recent print job

---

## Letterhead Management

Upload company letterhead documents:

1. Go to **Content > Letterheads**
2. Click **Upload**
3. Select a PDF or image file
4. Set as default or assign to specific groups

Letterheads are applied as background images in the Web App and on printed output.

---

## Reports & Auditing

### Viewing Reports

Go to **Reports** for pre-built reports:

| Report | Description |
|---|---|
| **Personal Usage** | Current user's print history |
| **Group Usage** | Print activity by group |
| **System Overview** | Total volume, costs, top users |
| **Financial Summary** | Total revenue/costs by period |
| **Printer Utilization** | Pages printed per printer |
| **User Balance** | Current balances and credit status |

### Generating Reports

1. Select the report type
2. Set the date range
3. Filter by user, group, or printer (optional)
4. Click **Generate**
5. View on screen or export to CSV/PDF

### Audit Log

The audit log contains all print activities:

- User, timestamp, action type
- Document name, page count, cost
- Source (IPP, Mail Print, Web Print)
- Destination printer
- IP address and user agent

Go to **Admin > Audit Log** to search and filter.

---

## Host Packages

The Admin Web App About section shows installed packages and versions:

- Java (JRE/JDK version)
- CUPS version
- PostgreSQL version (if used)
- Poppler version
- ImageMagick version
- And other system utilities

This helps verify that system dependencies meet requirements.

---

## Maintenance

### Starting/Stopping the Server

```bash
# Using systemd (if installed as a service)
sudo systemctl start printflowlite
sudo systemctl stop printflowlite
sudo systemctl restart printflowlite

# Or using the init script
sudo /etc/init.d/printflowlite start
sudo /etc/init.d/printflowlite stop
```

### Logs

Logs are available at:

```
/var/log/printflowlite/
├── printflowlite.log       # Main application log
├── printflowlite-audit.log # Audit trail
├── printflowlite-access.log # HTTP access log
```

Configure log levels in `log4j.properties`.

### Database Backup

```bash
# PostgreSQL
pg_dump -U postgres printflowlite > backup-$(date +%Y%m%d).sql

# Derby (embedded)
# Shut down the server, then copy the database directory
cp -r /var/lib/printflowlite/db /backup/db-$(date +%Y%m%d)
```

### Upgrading

1. Back up the database and configuration
2. Stop the server
3. Install the new version
4. Review migration notes
5. Start the server
6. Verify all functionality

---

## Troubleshooting Admin Issues

### LDAP Sync Fails

1. Check LDAP server is reachable
2. Verify bind DN and password
3. Test search filter manually with ldapsearch
4. Check logs for detailed error messages

### Mail Print Not Working

1. Verify SMTP settings and test connection
2. Check IMAP inbox for incoming mail
3. Ensure the IMAP user has access to the inbox
4. Check for SSL/TLS certificate issues

### Proxy Print Fails

1. Verify CUPS queue exists and is enabled
2. Check the printer is physically connected/online
3. Test printing directly from CUPS (`lp -d queue-name /path/to/file`)
4. Check SNMP settings for status monitoring

### High Server Load

1. Check for large SafePages backlogs
2. Review audit log for unusual activity
3. Consider increasing JVM heap size
4. Monitor database connection pool
5. Check disk space

### Users Can't Log In

1. Verify authentication method in Options
2. Check session timeout settings
3. Review LDAP/OAuth configuration
4. Check server SSL certificate validity
