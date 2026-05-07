# PrintFlowLite Monorepo

This is a Maven-based monorepo containing all PrintFlowLite packages.

## Package Dependencies

```
server
  ├── core
  │    └── common
  └── common

client
  └── common

cups-notifier
  └── core

ppd

ext

docker          (community-contributed, standalone)
android-print  (reference only — separate Android app)
```

## Build Order

When building from scratch, follow this order:

1. `common` — shared library
2. `core` — core business logic (depends on common)
3. `ext` — extension interface
4. `server` — web server (depends on core, common)
5. `client` — desktop client (depends on common)
6. `cups-notifier` — CUPS integration (depends on core)

## Non-Maven Packages

### docker

Community-contributed Docker deployment setup. Does not have a `pom.xml`. See `packages/docker/README.md` for usage.

### android-print

Reference package indicating that the Android Print Service app is maintained in a separate repository: `gitlab.com/savapage-android/savapage-android-print`. Fork and rebrand to create a PrintFlowLite Android app.

## Working with Git Submodules

Each package is a full git repository. The monorepo uses git worktrees or individual repos in the `packages/` directory.

### Syncing with Upstream (SavaPage)

The original SavaPage repositories are stored as bare clones in `tmp/`:

```
tmp/
├── savapage-server.git
├── savapage-core.git
├── savapage-common.git
├── savapage-client.git
├── savapage-ext.git
├── savapage-cups-notifier.git
├── savapage-ppd.git
├── community-docker.git          # Community Docker setup (from jboillot/savapage)
└── savapage-android-print.git   # Android Print app (from savapage-android/savapage-android-print)
```

To fetch latest upstream changes for a package:

```bash
cd packages/server
git fetch ../tmp/savapage-server.git
```

## CI/CD

GitHub Actions workflows in `.github/workflows/`:

- `build.yml` — Maven build, test, and validate on push/PR
- `release.yml` — GitHub release creation with build artifacts

Maven build profile can be activated per package:

- `mvn clean install -P build-server`
- `mvn clean install -P build-client`
- `mvn clean install -P build-all` (default)
