# PrintFlowLite Documentation

> Open Print Portal for Secure Printing, Pay-Per-Print, Delegated Print, Job Ticketing, Auditing and PDF Creation.

PrintFlowLite is a fork of [SavaPage](https://www.savapage.org/) — an open-source print management system — being developed further under a new name and architecture. It is licensed under the GNU Affero General Public License (AGPL) v3.

---

## Quick Navigation

### Getting Started
- [Installation Guide](getting-started.md) — System requirements, download, and installation
- [Architecture Overview](architecture.md) — How PrintFlowLite works: components, concepts, and workflows

### User Documentation
- [User Guide](user-guide.md) — How to use the PrintFlowLite Web App: printing, managing documents, proxy printing
- [Features Reference](features.md) — Complete list of features and capabilities

### Administrator Documentation
- [Admin Guide](admin-guide.md) — Server configuration, user management, proxy printers, options panel
- [Security](security.md) — Authentication, authorization, secure printing, and data protection

### Developer Documentation
- [API Reference](api-reference.md) — REST API endpoints, WebSocket (CometD) API, plugin/extension system
- [Development Guide](development.md) — Building from source, IDE setup, code standards

### Project
- [Roadmap](roadmap.md) — Project goals, version history, and upcoming features
- [Troubleshooting](troubleshooting.md) — Common issues and solutions

---

## What is PrintFlowLite?

PrintFlowLite is a Java EE web application with a server-side rendered UI (Apache Wicket + jQuery Mobile), providing a comprehensive print portal with the following capabilities:

| Capability | Description |
|---|---|
| **Secure Pull Printing** | Users release print jobs at any connected printer using NFC card or web login |
| **Pay-Per-Print** | Financial tracking and cost allocation per user, group, or department |
| **Delegated Printing** | Print on behalf of other users (print proxy) |
| **Job Ticketing** | Specify finishing options like staple, punch, bind for large print jobs |
| **Auditing** | Complete print activity logs and reporting |
| **PDF Creation** | Convert and export documents to PDF with metadata |

### Supported Print Methods

PrintFlowLite accepts print jobs through multiple channels:

- **Driver Print** — IPP (Internet Printing Protocol) or JetDirect from any desktop OS
- **AirPrint / Android Print** — Native mobile printing from iOS and Android devices
- **Mail Print** — Email documents as attachments to a dedicated inbox
- **Web Print** — Upload files directly through the web interface
- **File Upload** — Drag-and-drop PDF creation from the web app

### Who is it for?

PrintFlowLite is designed for:

- **Organizations** needing secure print release across multiple devices
- **Universities and schools** requiring print quota management
- **Libraries and public spaces** offering controlled printing
- **Businesses** tracking printing costs per department or project
- **IT departments** requiring print auditing and compliance

---

## Version

Current version: **1.7.0**

> This documentation is adapted from the [SavaPage User Manual](https://www.savapage.org/docs/manual/) (Version 1.7.0-rc), which is licensed under Creative Commons Attribution-ShareAlike 4.0. PrintFlowLite documentation is provided under the same license.

---

## Contributing to Docs

Documentation improvements are welcome. Please open an issue or submit a pull request on the [PrintFlowLite GitHub repository](https://github.com/YOUR_GITHUB/PrintFlowLite).
