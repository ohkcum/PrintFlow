# PrintFlowLite Features

PrintFlowLite provides a comprehensive set of print management features. This document gives an overview of all capabilities.

---

## Web Application Types

PrintFlowLite provides **7 specialized Web Applications**, each mounted at a distinct URL path:

| WebApp | URL | Purpose |
|--------|-----|---------|
| **Admin WebApp** | `/admin` | System administration, configuration, user management |
| **User WebApp** | `/user` | End-user print portal — SafePages, printing, account |
| **Print Site WebApp** | `/printsite` | Public print station — card tap, user registration |
| **Job Tickets WebApp** | `/jobtickets` | Dedicated job ticket management for print shops |
| **Mail Tickets WebApp** | `/mailtickets` | Email-based job ticket submission |
| **Payment WebApp** | `/payment` | Standalone payment/refill portal |
| **POS WebApp** | `/pos` | Point-of-Sale terminal for manual top-up |

Each WebApp has its own set of Wicket pages, JavaScript, and CSS, while sharing the core business logic in `packages/core`.

---

## Print Input Methods

### Driver Print (IPP / JetDirect)

Any desktop system can print to PrintFlowLite using a PostScript driver. Both IPP (port 631) and JetDirect (port 9100) protocols are supported.

- **IPP/IPPS** — Recommended for macOS, Linux, and modern Windows
- **JetDirect** — Recommended for Windows clients
- Install the `PRINTFLOWLITE.ppd` driver from the PrintFlowLite About section
- IPP options from PostScript are mapped automatically

### AirPrint

macOS and iOS users can print to PrintFlowLite natively using AirPrint without any driver installation. PrintFlowLite advertises via Avahi/mDNS. Chrome OS devices also use IPP natively for printing.

### Android Print Service

Android devices print to PrintFlowLite using the Android Print Service API. The PrintFlowLite Android app is maintained in a **separate repository**:

> **Repository**: [gitlab.com/savapage-android/savapage-android-print](https://gitlab.com/savapage-android/savapage-android-print) (fork and rebranding required for PrintFlowLite)

The Android app is available on F-Droid and discovers PrintFlowLite servers via mDNS/Bonjour.

### Mail Print

Users email documents as attachments to a dedicated PrintFlowLite inbox. The server monitors via IMAP and imports jobs into SafePages.

- Supports multiple inbox addresses (personal, shared)
- PDF, DOC, DOCX, ODT, images, and other common formats
- Sender is authenticated via IMAP login

### Web Print

Users upload files directly through the web interface. Supports:

- Drag-and-drop file upload (Dropzone)
- Multiple file selection
- PDF, DOC, DOCX, ODT, images, and more

### File Upload (SafePages Creation)

Users can upload files from within the Web App to create SafePages without printing.

---

## SafePages (Personal Print Queue)

SafePages is the personal user space where all submitted print jobs accumulate.

| Feature | Description |
|---|---|
| **Preview** | View all pages as thumbnails |
| **Zoom** | Zoom in on individual pages |
| **Delete Pages** | Remove unwanted pages before printing |
| **Page Range** | Select specific pages or page ranges |
| **Letterhead** | Apply company letterhead as background |
| **Reorder** | Drag pages to change print order |
| **Page Drawing** | Annotate pages with drawing tools (Fabric.js canvas) |
| **Job Ticketing** | Specify finishing options (staple, punch, bind) |

---

## Print Output Methods

### Proxy Print

Print selected SafePages to any physical printer managed by PrintFlowLite via local CUPS queues.

- No driver needed on client workstation
- Printer options (duplex, paper size, trays) selectable in Web App
- Supports all finishing options

### Fast Print Mode

Quick single-copy printing with default options. Ideal for simple print jobs where user interaction is not needed.

### Direct Print Release

Print directly from a terminal without web login. Authentication via NFC card reader.

### IPP Routing

Route IPP print jobs through PrintFlowLite to external printers. Supports:

- Terminal queues (user taps card, selects printer)
- Printer queues (direct routing)
- Hold queues (job held for approval)
- Job Ticket print queues

### PDF Export

Download SafePages as a PDF document with:

- Configurable metadata (title, author, subject, keywords)
- Password encryption
- Custom page range

### Email PDF

Send SafePages as a PDF attachment via email.

### OpenPGP Signing

Digitally sign exported PDFs with OpenPGP keys.

---

## Eco Print

Reduce toner and ink consumption with eco-friendly print options:

- **Grayscale** — Convert color documents to grayscale
- **Eco Mode** — Reduce ink/toner usage
- **Duplex** — Automatic double-sided printing
- **N-up** — Multiple pages per sheet (2-up, 4-up, 6-up, 8-up, 9-up, 16-up)
- **Remove Graphics** — Strip images for draft printing
- **Color Page Detection** — Detect color vs B&W pages for accurate cost tracking

---

## Financial (Pay-Per-Print)

### Cost Tracking

Track print costs per user, group, or department:

- Configurable cost per page (black/white, color, duplex, etc.)
- Automatic deduction from user balance
- Low balance warnings
- Cost limits per user or group

### User Account Types

- **Personal Account** — Individual user balance
- **Shared Account** — Group/department pool with sub-accounts
- **Credit Transfer** — Transfer balance between users
- **Money Transfer** — Charge to credit card via payment gateway

### Transactions

- Print transaction history per user
- Daily/weekly/monthly reports
- Export to CSV

### Refill Options

- Manual refill by administrator
- Point-of-Sale (POS) WebApp for cash/card top-up
- Integration with payment gateways (via extension API)
- Voucher codes (create and redeem)

### Pagometers (Environmental Impact)

Track and display environmental savings:

- Trees saved
- Water saved
- Energy saved
- CO2 reduced

---

## User Management

### Authentication Methods

| Method | Description |
|---|---|
| **Local** | Username/password stored in PrintFlowLite database |
| **LDAP / Active Directory** | Corporate directory integration |
| **OAuth 2.0** | Google, GitHub, Keycloak, and custom OAuth providers |
| **NFC Card** | Physical card authentication at the terminal |
| **YubiKey** | Hardware OTP token authentication |
| **ID Number** | Login with a numeric ID |

### 2-Step Verification (TOTP)

Time-based One-Time Password (TOTP) support for enhanced security:

- QR code setup with authenticator apps (Google Authenticator, etc.)
- Secret code display for manual entry
- Recovery codes for account recovery
- TOTP + Telegram 2FA — receive codes via Telegram bot

### User Roles

| Role | Capabilities |
|---|---|
| **User** | Print, manage SafePages, proxy print |
| **Admin** | Full system configuration, user management |
| **Manager** | View and manage group members' print jobs |
| **Delegator** | Print on behalf of other users |

### User Creation

- Manual creation by admin
- Automatic creation on first LDAP/OAuth login
- Bulk import from CSV
- Self-registration (public signup with email verification)
- Synchronization from LDAP groups

### User Source Options

PrintFlowLite can integrate with multiple user directory types:

- **Internal** — Users managed in PrintFlowLite database
- **LDAP / Active Directory** — Corporate directory (Unix, OpenLDAP, FreeIPA, AD)
- **Custom User Source** — Custom program for user lookup
- **PaperCut** — Use PaperCut as the user directory

---

## Security

### Secure Pull Printing

Print jobs are held in SafePages until the user authenticates at a terminal or browser. This prevents sensitive documents from sitting unattended at the printer.

### Authentication at Print

- Web login authentication
- NFC card reader authentication at a terminal
- Delegation authentication (proxy printer selection)
- IP-based authentication for abstract users

### Data Protection

- HTTPS/TLS encryption (Jetty with self-signed or custom keystore)
- Secure session management (HttpOnly cookies, configurable timeout)
- Audit logging of all print activities
- GDPR-compliant data handling (export, erasure)
- Rate limiting (DoS protection via Jetty DoSFilter)
- Inet access filter (IP whitelist/blacklist)

### Network Security

- Firewall-protected server
- Intrusion detection ready (audit logs)
- CUPS access control via `/etc/cups/cupsd.conf`
- TLS for LDAP, SMTP, IMAP connections

### Maintenance Mode

Block all access except admin login for maintenance operations.

---

## Proxy Printers

Proxy printers are physical printers managed by PrintFlowLite through local CUPS queues.

### Features

- **CUPS Integration** — Any CUPS queue becomes a Proxy Printer automatically
- **SNMP Monitoring** — Track printer status, toner levels, paper supply
- **DNS-SD Discovery** — Auto-discover printers on the network
- **IPP Everywhere** — Support for IPP Everywhere certified printers
- **Printer Groups** — Group printers by location or type

### Job Ticket Support

For large print jobs, users can specify finishing options:

- Staple (top-left, top-right, dual, saddle)
- Punch (2-hole, 3-hole, 4-hole)
- Bind (tape bind, thermal bind, spiral)
- Fold (tri-fold, Z-fold, booklet)
- Paper source selection
- Media type (plain, glossy, cardstock)

---

## PDF Processing

PrintFlowLite has extensive PDF capabilities:

- **PDF Creation** — iText and PDFBox for PDF generation
- **PDF Repair** — Automatically repair malformed PDFs
- **PDF Verification** — veraPDF validation
- **PDF Optimization** — Compress PDF files
- **PDF Encryption** — Password-protect PDFs
- **PDF to Booklet** — Create booklet-format PDFs
- **LibreOffice Conversion** — Convert DOC/DOCX/XLS/PPT/ODT/ODS/ODP to PDF via LibreOffice headless
- **PDF PGP Verification WebApp** — Verify OpenPGP signatures on PDFs

---

## Auditing & Reporting

### Audit Logs

Complete trail of all print activities:

- User, timestamp, document name, page count, cost
- Source (IPP, Mail Print, Web Print)
- Proxy printer used
- Status (submitted, printed, deleted, expired)

### Reports

- **Personal** — User's own print history
- **Group** — Print activity by group/department
- **System** — Overall print volume and costs
- **Financial** — Revenue, costs, balance summaries
- **Export** — CSV and PDF export via JasperReports

### JasperReports Integration

PrintFlowLite uses JasperReports for generating printable reports with 20+ report templates.

### Atom Feed

RSS/Atom feed publishing of system events for integration with monitoring tools.

---

## Internationalization (i18n)

PrintFlowLite supports multiple languages. The user interface and email notifications adapt to the user's preferred language.

Language packs are available for download from the Admin Web App.

---

## Notifications

PrintFlowLite sends notifications via multiple channels:

| Channel | Events |
|---|---|
| **Web App** | New SafePages arrived, low balance, job completed |
| **Email** | Job status, balance alerts, admin reports |
| **Desktop Client** | Real-time push notifications via CometD |
| **Telegram Bot** | 2FA codes, balance alerts, print job notifications |
| **Atom Feed** | System event feed for external monitoring |

---

## Extension System

PrintFlowLite provides a plugin API via `packages/ext` for extending functionality:

| Extension Type | Description |
|---|---|
| **Payment Processor** | Integrate external payment gateways |
| **OAuth Provider** | Add custom OAuth 2.0 authentication sources |
| **Notification Handler** | Custom notification channels |
| **Print Ticket Validator** | Custom validation of job tickets |
| **User Source** | Custom user directory program |
| **User Sync** | Custom user synchronization program |
| **PaperCut Integration** | Bi-directional sync with PaperCut MFPS |
| **IPP Routing Plugin** | Custom IPP routing handlers |

### PaperCut Integration

PrintFlowLite integrates with PaperCut MFPS in two directions:

- **PrintFlowLite as PaperCut source** — Sync users to PaperCut
- **PaperCut as PrintFlowLite source** — Use PaperCut user directory

---

## Deployment Options

### Traditional (Systemd)

Install as a systemd service on GNU/Linux (Debian, RHEL, openSUSE).

See [Getting Started](getting-started.md) for installation instructions.

### Docker

PrintFlowLite can be deployed in Docker containers. Community-contributed Docker setup available in `packages/docker/`.

Features:
- PrintFlowLite server + PostgreSQL in Docker Compose
- CUPS print server
- Persistent volumes for data, logs, and configuration
- Supervisord for managing multiple processes

### Cloud / Kubernetes

SavaPage has community support for Podman/Container on NethServer 8. PrintFlowLite can be adapted for Kubernetes deployment.

---

## Configuration Options

The Admin Web App provides a comprehensive Options panel organized into categories:

1. **User Source** — LDAP/AD settings, user sync, PaperCut, custom source
2. **User Authentication** — Login method, session timeout, 2FA, YubiKey
3. **Self-Registration** — Public user signup settings
4. **Mail** — SMTP/IMAP for Mail Print and notifications
5. **Web Print** — Allowed file types, size limits
6. **Internet Print** — Public IPP settings, security
7. **Proxy Print** — CUPS integration, default options, delegation
8. **Eco Print** — Default eco settings
9. **Financial** — Cost rates, balance limits, vouchers, POS
10. **Telegram** — Telegram bot configuration for 2FA and notifications
11. **Atom Feed** — Feed publishing settings
12. **Backups** — Automatic backup schedule
13. **Advanced** — Fine-grained tuning via config editor (JMX, pagometers, locale)

---

## Appendix: Supported File Types

| Category | Formats |
|---|---|
| **Printable** | PDF, PostScript, PCL |
| **Documents** | DOC, DOCX, ODT, RTF, TXT |
| **Spreadsheets** | XLS, XLSX, ODS |
| **Presentations** | PPT, PPTX, ODP |
| **Images** | PNG, JPEG, TIFF, BMP, GIF, HEIF |
| **Web** | HTML, URL (rendered via wkhtmltopdf or LibreOffice) |
| **Vector** | SVG (converted to raster via rsvg-convert) |
