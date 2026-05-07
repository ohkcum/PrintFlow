# PrintFlowLite Architecture

This document describes the high-level architecture of PrintFlowLite, including its key concepts, components, and data flows.

---

## Key Concepts

### Print Server

A print server hosts print queues and shares printer resources to client workstations. PrintFlowLite is a print server that aggregates multiple physical printers under one virtual print queue. All jobs flow through PrintFlowLite before reaching physical printers, enabling auditing, cost tracking, and secure release.

### Print Queue

A print queue is a first-in-first-out queue holding all jobs pending on a given printer. PrintFlowLite virtual queues redirect print jobs to the originating user's personal queue called **SafePages**. The PrintFlowLite Web App is the viewport on these SafePages.

### SafePages

SafePages is the PrintFlowLite term for a personal user space with accumulated jobs from PrintFlowLite print queues. Users can preview, edit, delete pages, apply letterheads, and select output printers before releasing jobs.

### Proxy Printer

A Proxy Printer is a physical printer available in the PrintFlowLite Web App for printing selected SafePages. Using a Proxy Printer does **not** require its printer driver on the client workstation. Proxy Printer queues are CUPS queues on the PrintFlowLite host and are not shared on the local network. They can only be selected in the PrintFlowLite Web App for pass-through printing.

### User ID / Username

In a multi-user environment, users log in using a username and password, often managed by Active Directory or LDAP. PrintFlowLite uses this identity for authentication, authorization, and auditing.

### Information Provider

A provider is a software component responsible for providing information to the Application Server. PrintFlowLite uses:
- **Print Provider** — IPP, JetDirect, IMAP (Mail Print), HTTP upload (Web Print)
- **User Directory Provider** — LDAP, NIS, local database
- **Authentication Provider** — Local, LDAP, OAuth
- **CUPS Information Provider** — Local printer discovery

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           PrintFlowLite                              │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   Application Server (Java EE)                │  │
│  │   ┌─────────────────┐  ┌──────────────────┐  ┌────────────┐  │  │
│  │   │  Apache Wicket  │  │  REST API (JAX-RS)│  │  CometD   │  │  │
│  │   │  Web Framework  │  │  /restful/v1/*   │  │  WebSocket│  │  │
│  │   └─────────────────┘  └──────────────────┘  └────────────┘  │  │
│  │   ┌──────────────────────────────────────────────────────┐  │  │
│  │   │              Business Logic & Services               │  │  │
│  │   └──────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                  Persistence Layer (JPA/Hibernate)            │  │
│  │              ┌─────────────────┐  ┌─────────────────┐       │  │
│  │              │ Apache Derby    │  │  PostgreSQL     │       │  │
│  │              │ (embedded, dev) │  │ (production)     │       │  │
│  │              └─────────────────┘  └─────────────────┘       │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    Print Server (IPP / JetDirect)              │  │
│  │            CUPS Integration via cups-notifier (C)             │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Desktop OS    │  │  Mobile OS      │  │     Email       │
│ (IPP/JetDirect) │  │ (AirPrint/Android│  │   (SMTP/IMAP)   │
│  + Browser      │  │   Print Service)│  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────────────┐
│                 PrintFlowLite Web Application                │
│   (jQuery Mobile / Adaptive HTML5 — any browser, any OS)    │
└─────────────────────────────────────────────────────────────┘
```

---

## 7 Web Application Types

PrintFlowLite provides **7 distinct Web Applications**, each serving a specific purpose and audience. All share the same backend business logic but have their own UI layer built with Apache Wicket.

| WebApp | Mount Path | ACL Role | Purpose |
|--------|-----------|----------|---------|
| **Admin WebApp** | `/admin` | `ADMIN` | System administration, configuration, user management, reports |
| **User WebApp** | `/user` | `USER`, `MANAGER`, `DELEGATOR` | End-user print portal — SafePages, printing, account balance |
| **Print Site WebApp** | `/printsite` | `PRINT_SITE_USER` | Public print station — card tap, user registration, limited options |
| **Job Tickets WebApp** | `/jobtickets` | `JOB_TICKET Issuer`, `ADMIN` | Dedicated job ticket management for print shops |
| **Mail Tickets WebApp** | `/mailtickets` | `MAIL_TICKET Issuer`, `ADMIN` | Email-based job ticket submission via IMAP |
| **Payment WebApp** | `/payment` | `USER`, `MANAGER`, `DELEGATOR` | Standalone payment/refill portal (separate login) |
| **POS WebApp** | `/pos` | `ADMIN`, `MANAGER` | Point-of-Sale terminal for cash/card top-up by staff |

Each WebApp is implemented as a separate set of Wicket pages with its own JavaScript and CSS bundles. The `WebAppTypeEnum` enum in `packages/core` defines all types.

---

## Component Overview

### packages/server

The main web application built with Apache Wicket and Jetty. Handles HTTP requests, manages web sessions, renders HTML pages, exposes REST endpoints, and manages WebSocket (CometD) connections for real-time updates.

**Key technologies**: Apache Wicket 9.22, Jetty 10.0, CometD 6.0, Jersey JAX-RS 2.45, jQuery Mobile 1.4

### packages/core

Core business logic and persistence layer. Contains all domain models, services, and JPA/Hibernate entity definitions. Shared by both server and CUPS notifier.

**Key technologies**: JPA/Hibernate, Apache Derby, PostgreSQL JDBC, iText (PDF), JasperReports

### packages/common

Shared library containing common utilities, constants, and version information used by all modules.

### packages/client

Desktop client application (Java Swing). Allows end-users to authenticate, receive push notifications via CometD, and interact with PrintFlowLite from their workstation. Provides system tray integration and automatic startup.

**Key technologies**: Java Swing, CometD client

### packages/ext

Extension interface (plugin API). Defines the extension points for adding custom functionality such as payment processors, notification handlers, OAuth providers, and print ticket validators.

### packages/cups-notifier

C/C++ CUPS notifier binary that bridges CUPS job events to the PrintFlowLite server via XML-RPC. Runs as a CUPS backend or notifier hook.

### packages/ppd

PostScript Printer Description (PPD) files and Windows driver setup. Provides the `PRINTFLOWLITE.ppd` driver optimized for PrintFlowLite printing, plus Windows installer scripts.

---

## User Workflow

### End-User Perspective

1. User opens a browser and logs into PrintFlowLite with credentials (local, LDAP, or OAuth)
2. User prints a document from their editor to the PrintFlowLite Network Printer
3. Printed pages appear as thumbnails in the browser in real-time
4. User browses thumbnails, zooms in, deletes unwanted pages
5. User selects letterhead background and configures print settings
6. User selects a Proxy Printer (e.g., the MFD down the hall)
7. User presses **Print** — job is released to the physical printer
8. User can also download the result as a PDF with metadata and encryption

### Technical Perspective

1. Client workstation sends print job via IPP or JetDirect to PrintFlowLite
2. Print Provider captures the job, extracts user identity and queue info
3. Application Server approves the request, stores job in SafePages
4. Server signals the user's browser session via WebSocket (CometD)
5. Web App in browser renders the new pages and awaits user input
6. User edits (delete, letterhead) are sent to Application Server
7. When user presses Print, Application Server composes the final job
8. Application Server sends the job to the Proxy Printer via local CUPS
9. For PDF export, Application Server renders and streams the PDF

---

## Data Flow Diagram

```
Print Source
     │
     ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   IPP / AirPrint│     │   Mail Print    │     │   Web Print     │
│   JetDirect     │     │   SMTP → IMAP   │     │   HTTP Upload   │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Print Provider Service                     │
│           (captures print jobs from all sources)             │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                  Application Server (Core)                   │
│  ┌──────────┐  ┌───────────┐  ┌──────────┐  ┌──────────┐ │
│  │  Audit   │  │  Financial│  │  User    │  │  Print   │ │
│  │  Service │  │  Service  │  │  Service │  │  Service │ │
│  └──────────┘  └───────────┘  └──────────┘  └──────────┘ │
│                      │                                        │
│                      ▼                                        │
│              ┌──────────────┐                                 │
│              │  SafePages   │ ← User's Personal Queue        │
│              │  Storage     │                                 │
│              └──────────────┘                                 │
└────────────────────────────┬────────────────────────────────┘
                             │
         ┌───────────────────┼────────────────────┐
         │                   │                    │
         ▼                   ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  Web App        │  │  CometD Push    │  │  PDF Export     │
│  (Browser)      │  │  (Real-time)    │  │  (Download)     │
└─────────────────┘  └─────────────────┘  └─────────────────┘
                             │
                             ▼
                 ┌─────────────────────────┐
                 │  Proxy Printer (CUPS)   │
                 │  Physical Printer       │
                 └─────────────────────────┘
```

---

## Communication Protocols

| Protocol | Purpose | Direction |
|---|---|---|
| HTTP/HTTPS | Web App, REST API, Web Print | Client → Server |
| IPP/IPPS | Print job submission | Client → Server |
| JetDirect (TCP 9100) | Print job submission | Client → Server |
| SMTP | Mail Print submission | Mail Server → Server |
| IMAP | Mail Print retrieval | Server → Mail Server |
| WebSocket (CometD) | Real-time push notifications | Bidirectional |
| XML-RPC | CUPS notifier → server | Notifier → Server |
| LDAP/LDAPS | User directory lookup | Server → LDAP |
| SNMP | Proxy printer status monitoring | Server → Printer |

---

## Directory Structure

```
PrintFlowLite/
├── .github/
│   └── workflows/       # GitHub Actions CI/CD pipelines
├── packages/
│   ├── server/          # Jetty + Wicket web application
│   │   └── src/main/
│   │       ├── java/    # Java source (Wicket pages, REST, CometD, JSON API)
│   │       └── webapp/  # HTML templates, CSS, JS, images
│   ├── core/            # Business logic and persistence
│   │   └── src/main/
│   │       └── java/    # Domain models, services, JPA entities
│   ├── common/          # Shared utilities and constants
│   ├── client/          # Desktop Swing client application
│   ├── ext/             # Plugin/extension API
│   ├── cups-notifier/   # C/C++ CUPS notifier binary
│   ├── ppd/             # PostScript driver files (PPD)
│   ├── docker/           # Community Docker deployment setup
│   └── android-print/    # Android Print Service reference
├── docs/                # PrintFlowLite documentation
└── tmp/                 # Git bare repos (upstream SavaPage)
```
