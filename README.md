# PrintFlow

Full TypeScript/Node.js rewrite of PrintFlowLite - a print management system with user portal, financial tracking, job tickets, and more.

## Tech Stack

- **Backend**: Fastify 5 + Drizzle ORM + PostgreSQL 16
- **Frontend**: Next.js 15 + React 19 + shadcn/ui + TanStack Query
- **Session/Queue**: ioredis + BullMQ
- **Monorepo**: Turbo + pnpm

## Prerequisites

- Node.js 18+ 
- pnpm 8+
- PostgreSQL 16
- Redis (optional, for production)

## Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/ohkcum/PrintFlow.git
   cd PrintFlow
   ```

2. **Install dependencies**
   ```bash
   pnpm install
   ```

3. **Setup environment variables**
   ```bash
   cp .env.example .env.local
   # Edit .env.local with your database and configuration values
   ```

4. **Setup database**
   ```bash
   # Run the initialization SQL script
   psql -U postgres -d postgres -f scripts/init-db.sql
   
   # Run seed data
   pnpm --filter @printflow/db seed
   ```

5. **Start development servers**
   ```bash
   # Start API server (port 3001)
   pnpm --filter @printflow/api dev
   
   # Start web frontend (port 3000)
   pnpm --filter @printflow/web dev
   ```

6. **Access the application**
   - Frontend: http://localhost:3000
   - API: http://localhost:3001
   - Default admin user: admin / admin123

## Project Structure

```
PrintFlow/
├── apps/
│   ├── api/          # Fastify REST API
│   ├── web/          # Next.js 15 frontend
│   ├── worker/       # BullMQ job processor
│   └── client/       # Tauri 2 desktop client (scaffold)
├── packages/
│   ├── auth/         # Auth service (session, password, TOTP)
│   ├── common/       # Shared types, Zod schemas, enums
│   ├── db/           # Drizzle ORM schema + migrations
│   └── tsconfig/     # Shared TypeScript config
├── infra/
│   └── docker/       # docker-compose.yml, Dockerfiles
└── scripts/          # Dev scripts
```

## Development

### Running specific apps

```bash
# API only
pnpm --filter @printflow/api dev

# Web only
pnpm --filter @printflow/web dev

# Worker only
pnpm --filter @printflow/worker dev

# Desktop client (Tauri)
pnpm --filter @printflow/client dev
```

### Database migrations

```bash
# Generate migration
pnpm --filter @printflow/db db:generate

# Apply migration
pnpm --filter @printflow/db db:migrate

# Push schema changes (dev only)
pnpm --filter @printflow/db db:push
```

### Testing

```bash
# Run all tests
pnpm test

# Run tests for specific package
pnpm --filter @printflow/auth test
```

## Production Deployment

1. **Build all packages**
   ```bash
   pnpm build
   ```

2. **Use Docker Compose**
   ```bash
   cd infra/docker
   docker-compose up -d
   ```

## Current Status

- ✅ Phase 1: Infrastructure & Auth
- ✅ Phase 2: User Portal & Print Pipeline
- ⚠️ Phase 3: Business Logic & Integration (87.5% complete - PGP is stub)
- ⚠️ Phase 4: UX, Reports & Desktop (25% complete - routers commented out)

See [REFACTOR-PLAN.md](./REFACTOR-PLAN.md) for detailed feature status.

## Known Issues

- Phase 4 routers (reports, pos, qr, telegram, oauth, snmp) are commented out due to missing database schemas
- PGP service is a stub implementation (needs openpgp library)
- Role-based access checks are temporarily disabled for development

## License

MIT
