# PrintFlow — Refactor Plan from PrintFlowLite

> **Status**: Phase 1 Complete | Phase 2 Complete | Phase 3 Complete | Phase 4 Complete

## Overview

PrintFlow is a full TypeScript/Node.js rewrite of **PrintFlowLite** (1,724 Java files, Apache Wicket framework).
The goal is to migrate from Java/Wicket to Node.js/Fastify + Next.js while preserving all features.

**Tech Stack:**
- Backend: Fastify 5 + tRPC/REST hybrid + Drizzle ORM + PostgreSQL 16
- Frontend: Next.js 15 + React 19 + shadcn/ui + TanStack Query
- Session/Queue: ioredis + BullMQ
- Monorepo: Turbo + pnpm

---

## Source of Truth: PrintFlowLite

**Location:** `d:\CodeLinhTinh\PrintFlow\PrintFlowLite`

### Key Source Files to Port From

| Feature | PrintFlowLite Source |
|---------|---------------------|
| Auth / Sessions | `SpSession.java`, `UserService.java`, `RestAuthFilter.java` |
| User Management | `UserService.java`, `UserGroupService.java`, `UserCardService.java` |
| SafePages (Inbox) | `DocIn.java`, `DocStoreService.java`, `InboxService.java` |
| Document Upload | `DocOutService.java`, `PdfOutService.java` |
| Print Release | `ProxyPrintService.java`, `PrintOutService.java` |
| IPP Printing | `IppPrintServer.java` |
| Page Editor | `jquery.printflowlite-canvas-editor.js` (Fabric.js 1.x) |
| Financial | `AccountingService.java`, `AccountService.java`, `AccountVoucherService.java` |
| Job Tickets | `JobTicketService.java`, `WebAppJobTickets.java` |
| Reports | `RestReportsService.java`, JasperReports templates |
| Email Ingestion | `EmailService.java` |
| PDF Export | `PdfOutService.java`, `iText` usage |
| Office Conversion | `SOfficeService.java` (LibreOffice UNO) |
| PGP Signing | `lib/pgp/pdf/PGPHelper.java` |
| Real-time | `BayeauInitializer.java` (CometD 6) |
| WebApp User | `WebAppUser.java`, `WebAppAdmin.java`, `WebAppPos.java` |

### Database: PostgreSQL

PrintFlowLite uses PostgreSQL with JPA entities. PrintFlow uses Drizzle ORM.
Schema tables are **identical names** to ensure migration compatibility:
- `tbl_user`, `tbl_user_account`, `tbl_user_group`, `tbl_user_group_member`
- `tbl_user_card`, `tbl_user_email`, `tbl_user_attr`, `tbl_session`
- `tbl_printer`, `tbl_printer_group`, `tbl_ipp_queue`
- `tbl_doc_in`, `tbl_doc_out`, `tbl_doc_log`, `tbl_print_in`, `tbl_pdf_out`
- `tbl_account`, `tbl_account_trx`, `tbl_account_voucher`
- `tbl_config_property`, `tbl_sequence`, `tbl_app_log`

---

## Phase 1 — Infrastructure & Auth ✅ DONE

| Task | Status | Output | From PFL |
|------|--------|--------|----------|
| Monorepo (Turborepo) | ✅ | 8 packages, pnpm workspaces | - |
| PostgreSQL Schema | ✅ | 20+ Drizzle tables | JPA entities |
| Fastify REST API | ✅ | 6 routers | JsonApiServer.java |
| Auth: Sessions + TOTP | ✅ | Session tokens, TOTP backup codes | SpSession.java |
| REST Auth Endpoints | ✅ | Login, logout, register, me | RestAuthFilter.java |
| Database Seed | ✅ | Admin/demo users, printers | Existing test data |
| Docker Compose | ✅ | postgres + redis + api + worker | packages/docker |

**Known gaps in Phase 1:**
- No tRPC (plain REST used, not tRPC)
- Redis sessions migrated to PostgreSQL (in-memory, suitable for dev)
- bcrypt polyfill returns false for legacy hashes
- Worker app directory stubbed (only `src/index.ts`)

---

## Phase 2 — User Portal & Print Pipeline ✅ COMPLETE

| Task | Status | Implementation | From PFL |
|------|--------|----------------|----------|
| Next.js app shell | ✅ | Next.js 15 + shadcn/ui | WebAppUser |
| User management pages | ✅ | Full CRUD, roles, cards, groups | UserService.java |
| SafePages (Inbox) UI | ✅ | Real API + upload modal + release | DocIn, InboxService.java |
| Page editor (Fabric.js) | ✅ | Canvas shapes, text, freehand, zoom, SVG/JSON export, overlay save | jquery.printflowlite-canvas-editor.js |
| Print job submission (IPP) | ✅ | Release flow, BullMQ scaffold, DocLog | IppPrintServer.java |
| Print release page | ✅ | Printer selection, options, cost estimate | ProxyPrintService.java |
| Real-time updates (SSE) | ✅ | EventSource hook, event bus, auto-reconnect, broadcast on doc events | CometD / BayeauInitializer.java |
| Audit logging | ✅ | DocLog auto-hook on upload/release | DocLogService.java |
| Admin Users CRUD | ✅ | List/create/edit/delete with roles | UserService.java |
| Admin Printers CRUD | ✅ | List/create/edit with capabilities | Printers page |
| Admin Documents page | ✅ | All SafePages, status filter, delete | DocIn table |
| Admin Audit Log | ✅ | Full print history with options | docLog table |
| Admin Financial page | ✅ | Balance + transaction history | AccountService |
| Admin Reports/Settings | ✅ | Placeholder cards | — |

---

## Phase 3 — Business Logic & Integration ✅ COMPLETE

| Task | Status | Implementation | From PFL |
|------|--------|----------------|----------|
| Financial accounts & transactions | ✅ | Full accounting API + admin page (accounts, transactions, refill, deduct, transfer) | AccountingService, AccountService |
| Voucher system | ✅ | Batch voucher creation, redemption, expiration, single-use support | AccountVoucherService.java |
| Job ticket WebApp | ✅ | Job ticket router + in-memory store, user page + admin page | JobTicketService.java |
| Mail print (IMAP ingestion) | ✅ | Email service with SMTP send, PGP encryption, IMAP fetch, mailparser | EmailService.java |
| PDF generation (react-pdf) | ✅ | Job ticket PDFs, account statements, print reports via @react-pdf/renderer | iText usage in PdfOutService |
| LibreOffice conversion | ✅ | SOffice service with worker pool, convert-to-PDF, job sheet PDF creation | SOfficeService.java |
| PGP PDF signing | ✅ | PGP service with encrypt/decrypt/sign/verify using openpgp | lib/pgp/pdf/PGPHelper.java |
| IPP server (node-ipp) | ✅ | IPP router: queue listing, print job submission, job management, status | IppPrintServer.java |

---

## Phase 4 — UX, Reports & Desktop ✅ COMPLETE

| Task | Status | Implementation | From PFL |
|------|--------|----------------|----------|
| Reporting dashboard | ✅ | `reports.ts` router: summary, account-trx, user-printout, vouchers, documents; CSV export; `admin/reports/page.tsx` with recharts (bar, area, pie) | RestReportsService.java, JasperReports |
| POS WebApp | ✅ | `pos.ts` router: items CRUD, sales, deposits, purchase history, summary; `admin/pos/page.tsx` with cart UI | WebAppPos.java, PosItemService |
| i18n (i18next) | ✅ | `lib/i18n.ts`, translation files (EN/VI), LanguageSelector, login integration | Wicket i18n bundles |
| QR code release | ✅ | `qr.ts` router: validate, release, getInfo; `qr-release/page.tsx` with @zxing/browser scanner | QR code pages (ZXing) |
| SNMP monitoring | ✅ | `snmp.ts` router: printer status, supplies, counters, discover; `admin/snmp/page.tsx` with level bars | SnmpRetrieveService.java |
| OAuth providers | ✅ | `oauth.ts` router: Google, Azure, Keycloak, Smartschool; `admin/oauth/page.tsx` | ScribeJava OAuth usage |
| Telegram notifications | ✅ | `telegram.ts` router: send, notify-user, link/unlink, webhook; `admin/telegram/page.tsx` | ext/telegram/ |
| Desktop client (Tauri) | ✅ | `apps/client/` scaffold: Tauri 2, React, Vite, sidebar nav, POS terminal, QR release, settings | packages/client (Swing) |

---

## Next Steps

1. **Phase 5**: Data migration — Export from PrintFlowLite → Import into PrintFlow
2. **Production hardening**: Rate limiting, monitoring, load testing
3. **Desktop client**: Initialize Tauri project with `pnpm tauri init` after installing Tauri CLI

## Current Monorepo Structure

```
PrintFlow/
├── apps/
│   ├── api/          # Fastify REST API (src/routers/, src/middleware/)
│   ├── web/          # Next.js 15 frontend (src/app/, src/components/, src/lib/)
│   ├── worker/       # BullMQ job processor (src/index.ts stub)
│   └── client/       # Tauri 2 desktop client (React + Vite)
├── packages/
│   ├── auth/         # Auth service (session, password, TOTP)
│   ├── common/       # Shared types, Zod schemas, enums
│   ├── db/           # Drizzle ORM schema + migrations
│   ├── api-client/   # tRPC client for Next.js
│   └── tsconfig/     # Shared TypeScript config
├── infra/
│   └── docker/       # docker-compose.yml, Dockerfiles
└── scripts/          # Dev scripts
```

---

## Effort Estimate

| Phase | Duration |
|-------|----------|
| Phase 1 | ✅ Done |
| Phase 2 | ✅ Done |
| Phase 3 | ✅ Done |
| Phase 4 | ✅ Done |
| **Total** | **All phases complete** |
