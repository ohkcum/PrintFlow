# PrintFlowLite API Reference

PrintFlowLite provides multiple APIs for programmatic access and integration.

---

## API Overview

PrintFlowLite has **three API layers**:

| API | Path | Type | Purpose |
|-----|------|------|---------|
| **JSON API** | `/api` | JSON (Wicket) | Primary API for web client interactions — 98+ operations |
| **REST API** | `/restful/v1` | REST (JAX-RS) | Standard REST endpoints for external integrations |
| **CometD** | `/cometd` | WebSocket | Real-time push events |

---

## JSON API (`/api`)

The JSON API is the primary API used by the web client. It is implemented as a Wicket page that handles JSON request/response.

**Base URL**: `https://your-printflowlite-server:8632/api`

**Content-Type**: `application/json`

### Authentication

The JSON API uses the same session as the web application. Users must be logged in via the web interface, or authenticate using:

**Basic Auth** (for API access):
```
Authorization: Basic base64(username:password_or_apikey)
```

**API Key** (admin-configured):
```
X-Api-Key: YOUR_API_KEY
```

### Request Format

```json
POST /api
{
  "jsonrpc": "2.0",
  "method": "user.get",
  "params": {
    "userId": 123
  },
  "id": 1
}
```

### Response Format

```json
{
  "jsonrpc": "2.0",
  "result": { ... },
  "id": 1
}
```

### Available Operations (98+)

The JSON API covers the following areas (each corresponds to a `Req*.java` request class):

#### User Management
| Method | Operation | Description |
|--------|----------|-------------|
| `user.get` | `ReqUserGet` | Get user details |
| `user.set` | `ReqUserSet` | Update user |
| `user.init-internal` | `ReqUserInitInternal` | Create internal user |
| `user.group-get` | `ReqUserGroupGet` | Get user's groups |
| `user.group-set` | `ReqUserGroupSet` | Set user's groups |
| `user.group-add-remove` | `ReqUserGroupsAddRemove` | Add/remove from groups |
| `user.delegate-groups-preferred` | `ReqUserDelegateGroupsPreferred` | Preferred delegation groups |
| `user.delegate-accounts-preferred` | `ReqUserDelegateAccountsPreferred` | Preferred delegation accounts |
| `user.quick-search` | `ReqUserQuickSearch` | Search users |
| `user.pin-erase` | `ReqUserPinErase` | Erase user PIN |
| `user.password-erase` | `ReqUserPasswordErase` | Erase user password |
| `user.secret-erase` | `ReqUserSecretErase` | Erase TOTP secret |
| `user.uuid-replace` | `ReqUserUuidReplace` | Replace user UUID |
| `user.attr-enable` | `ReqUserAttrEnable` | Enable/disable user attributes |
| `user.notify-account-change` | `ReqUserNotifyAccountChange` | Notify account change |
| `user.name-aliases-refresh` | `ReqUserNameAliasesRefresh` | Refresh name aliases |
| `user.home-clean` | `ReqUserHomeClean` | Clean user home |
| `user.totp-set` | `ReqUserTOTPSet` | Set TOTP |
| `user.totp-send-recovery-code` | `ReqUserTOTPSendRecoveryCode` | Send TOTP recovery code |
| `user.totp-replace` | `ReqUserTOTPReplace` | Replace TOTP |
| `user.telegram-set` | `ReqUserSetTelegramID` | Set Telegram ID |
| `user.telegram-test` | `ReqUserTestTelegramID` | Test Telegram ID |

#### User Registration
| Method | Operation | Description |
|--------|----------|-------------|
| `user-registration.create` | `ReqUserRegistration` | Create registration |
| `user-registration.permit` | `ReqUserRegistrationPermit` | Permit registration |

#### User Groups
| Method | Operation | Description |
|--------|----------|-------------|
| `user-group.quick-search` | `ReqUserGroupQuickSearch` | Search groups |
| `user-group.member-quick-search` | `ReqUserGroupMemberQuickSearch` | Search group members |

#### Card Readers
| Method | Operation | Description |
|--------|----------|-------------|
| `card-user-quick-search` | `ReqCardUserQuickSearch` | Card reader user search |

#### Devices
| Method | Operation | Description |
|--------|----------|-------------|
| `device.get` | `ReqDeviceGet` | Get device |
| `device.set` | `ReqDeviceSet` | Create/update device |
| `device.delete` | `ReqDeviceDelete` | Delete device |

#### Queues
| Method | Operation | Description |
|--------|----------|-------------|
| `queue.get` | `ReqQueueGet` | Get queue |
| `queue.set` | `ReqQueueSet` | Create/update queue |
| `queue.enable` | `ReqQueueEnable` | Enable/disable queue |

#### Printers
| Method | Operation | Description |
|--------|----------|-------------|
| `printer.get` | `ReqPrinterGet` | Get printer |
| `printer.set` | `ReqPrinterSet` | Create/update printer |
| `printer.print` | `ReqPrinterPrint` | Print document |
| `printer.print-site-user-set` | `ReqPrintSiteUserSet` | Set print site user |

#### Document Log
| Method | Operation | Description |
|--------|----------|-------------|
| `doc-log.refund` | `ReqDocLogRefund` | Refund document log |
| `doc-log.store-delete` | `ReqDocLogStoreDelete` | Delete from store |
| `doc-log.ticket-reopen` | `ReqDocLogTicketReopen` | Reopen ticket |

#### Account Transactions
| Method | Operation | Description |
|--------|----------|-------------|
| `account-trx.get` | `ReqAccountTrxGet` | Get account transactions |
| `account-trx.batch-create` | `ReqAccountTrxBatchCreate` | Batch create transactions |

#### Shared Accounts
| Method | Operation | Description |
|--------|----------|-------------|
| `shared-account.get` | `ReqSharedAccountGet` | Get shared account |
| `shared-account.set` | `ReqSharedAccountSet` | Create/update shared account |
| `shared-account.quick-search` | `ReqSharedAccountQuickSearch` | Search shared accounts |

#### Vouchers
| Method | Operation | Description |
|--------|----------|-------------|
| `voucher.batch-create` | `ReqVoucherBatchCreate` | Create voucher batch |
| `voucher.batch-delete` | `ReqVoucherBatchDelete` | Delete voucher batch |
| `voucher.batch-expire` | `ReqVoucherBatchExpire` | Expire voucher batch |
| `voucher.redeem` | `ReqVoucherRedeem` | Redeem voucher |
| `voucher.delete-expired` | `ReqVoucherDeleteExpired` | Delete expired vouchers |

#### System & Configuration
| Method | Operation | Description |
|--------|----------|-------------|
| `config-prop.get` | `ReqConfigPropGet` | Get config property |
| `config-props.set` | `ReqConfigPropsSet` | Set config properties |
| `system.mode-change` | `ReqSystemModeChange` | Change system mode |
| `db.backup` | `ReqDbBackup` | Database backup |
| `atom-feed.refresh` | `ReqAtomFeedRefresh` | Refresh Atom feed |
| `mail.test` | `ReqMailTest` | Test mail configuration |

#### Web Print
| Method | Operation | Description |
|--------|----------|-------------|
| `webprint.url-print` | `ReqUrlPrint` | Print URL |

---

## REST API (`/restful/v1`)

The REST API provides standard REST endpoints for external integrations. It is implemented using JAX-RS (Jersey).

**Base URL**: `https://your-printflowlite-server:8632/restful/v1`

**Content-Type**: `application/json`

### Authentication

REST API uses HTTP Basic Authentication:

```
Authorization: Basic base64(username:password_or_apikey)
```

### Available REST Services

The REST API provides the following service classes:

| Service | Description |
|----------|-------------|
| `RestSystemService` | System information, status, version |
| `RestDocumentsService` | Document management, upload, download |
| `RestReportsService` | Reporting endpoints |
| `RestFinancialService` | Financial transactions, balance |
| `RestTestService` | Test/debug endpoints |

### REST Endpoints

#### System (`/restful/v1/system`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/status` | System status |
| `GET` | `/version` | Server version |

#### Documents (`/restful/v1/documents`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | List documents |
| `POST` | `/upload` | Upload document |
| `GET` | `/{id}` | Get document |
| `DELETE` | `/{id}` | Delete document |

#### Reports (`/restful/v1/reports`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/usage` | Usage report |
| `GET` | `/financial` | Financial report |
| `GET` | `/export` | Export report |

#### Financial (`/restful/v1/financial`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/balance/{userId}` | Get user balance |
| `POST` | `/refill/{userId}` | Refill balance |
| `GET` | `/transactions/{userId}` | User transactions |

### Example REST Request

```bash
# Get system status
curl -X GET https://localhost:8632/restful/v1/system/status \
  -u admin:changeme

# Upload document
curl -X POST https://localhost:8632/restful/v1/documents/upload \
  -u admin:changeme \
  -F "file=@document.pdf"
```

---

## CometD WebSocket API

The CometD API provides real-time push notifications to connected clients.

**Endpoint**: `wss://your-printflowlite-server:8632/cometd/0.0`

### Connection

Connect to the CometD endpoint and authenticate:

```json
[
  {
    "channel": "/meta/handshake",
    "version": "1.0",
    "supportedConnectionTypes": ["websocket", "long-polling"],
    "ext": {
      "authentication": {
        "userName": "admin",
        "password": "password"
      }
    }
  }
]
```

### Subscribe to Channels

```json
[
  { "channel": "/meta/subscribe", "subscription": "/admin/**" },
  { "channel": "/meta/subscribe", "subscription": "/user/event" },
  { "channel": "/meta/subscribe", "subscription": "/device/event" },
  { "channel": "/meta/subscribe", "subscription": "/proxyprint/event" }
]
```

### Event Channels

| Channel | Description |
|---------|-------------|
| `/admin/**` | All admin events (wildcard subscription) |
| `/admin/config` | Configuration changes |
| `/admin/cups` | CUPS events |
| `/admin/db` | Database events |
| `/admin/ipp` | IPP print events |
| `/admin/mailprint` | Mail print events |
| `/admin/webprint` | Web print events |
| `/admin/nfc` | NFC/card events |
| `/admin/payment-gateway` | Payment events |
| `/admin/plugin` | Plugin events |
| `/admin/proxyprint` | Proxy print events |
| `/admin/scheduler` | Scheduler events |
| `/admin/system` | System events |
| `/admin/user` | User events |
| `/admin/user-sync` | User sync events |
| `/admin/webservice` | Web service events |
| `/user/event` | User-facing events (new SafePages, etc.) |
| `/device/event` | Device events |
| `/proxyprint/event` | Print job events |

### Event Format

```json
{
  "channel": "/admin/system",
  "data": {
    "level": "INFO",
    "topic": "SYSTEM",
    "message": "System started",
    "timestamp": "2025-03-01T14:22:00Z"
  }
}
```

---

## Other Protocols

### IPP Print Server

PrintFlowLite acts as an IPP server. IPP requests are handled by the `IppPrintServer` class mounted at `/printers`.

### XML-RPC

The CUPS notifier communicates with PrintFlowLite via XML-RPC. This is used for:
- NFC card reader events
- CUPS job events
- One-time authentication tokens
- Client app communication

### Atom Feed

PrintFlowLite publishes an Atom feed of system events. Configure in Options > Atom Feed.

### JSON-RPC (Legacy)

A JSON-RPC interface is also available for specific integrations.

---

## API Security

### Rate Limiting

API requests are subject to rate limiting configured in `server.properties`:

```properties
# DoS filter rate limiting
dosfilter.maxrequestsperuri=1000
dosfilter.maxrequestspersession=5000
```

### Authentication

- Use HTTPS for all API calls
- API keys should be rotated periodically
- Session-based auth for JSON API
- Basic Auth for REST API

### Error Responses

Standard error format:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable message"
  }
}
```

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `UNAUTHORIZED` | 401 | Not authenticated |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `VALIDATION_ERROR` | 400 | Invalid request |
| `RATE_LIMITED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Server error |
