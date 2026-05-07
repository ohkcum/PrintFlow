# PrintFlowLite Roadmap

This document outlines the project goals, current status, and planned development.

---

## Project Status

PrintFlowLite is a **fork of SavaPage** — an open-source print management system originally developed by Datraverse B.V. The fork was created to continue development under a new name and direction.

**Current Version:** 1.7.0

**Forked From:** SavaPage 1.7.0-rc

---

## Version History

### v1.7.0 (Current)

- Forked from SavaPage 1.7.0-rc
- Full codebase rebranding to PrintFlowLite
- Reorganization into Maven monorepo structure
- Package dependencies restructured

### Prior to Fork

SavaPage was actively developed from 2011 to 2024 with releases including:

- **v1.6** — Major UI improvements, mobile optimization
- **v1.5** — OAuth integration, REST API
- **v1.4** — CometD real-time push notifications
- **v1.3** — Mail Print, Web Print
- **v1.2** — Job ticketing, eco print
- **v1.1** — LDAP/AD integration
- **v1.0** — Initial release

---

## Development Goals

### Short Term (Near Future)

| Goal | Priority | Status |
|---|---|---|
| Stabilize build and test infrastructure | High | In Progress |
| Update dependencies (Jetty, Wicket, Hibernate) | High | Planned |
| Migrate from Java 8 to Java 17+ | Medium | Planned |
| Modernize build system (Java 17 module system) | Medium | Planned |
| Fix known issues from SavaPage upstream | Medium | Planned |

### Medium Term

| Goal | Priority | Description |
|---|---|---|
| **Web UI Modernization** | High | Migrate from jQuery Mobile to a modern frontend framework (React/Vue) |
| **API Overhaul** | High | Modern REST API with OpenAPI/Swagger documentation |
| **Cloud-Native Deployment** | Medium | Kubernetes Helm charts, cloud-native storage |
| **Multi-Tenant Support** | Medium | Support multiple organizations on one instance |
| **Improved Documentation** | Medium | This documentation suite |

### Completed

| Goal | Description |
|---|---|
| **Docker Support** | Community Docker setup in `packages/docker/` with docker-compose, Dockerfile, and supervisor config |
| **CI/CD Pipeline** | GitHub Actions workflows in `.github/workflows/` for build, test, and release |

### Long Term

| Goal | Priority | Description |
|---|---|---|
| **Plugin Marketplace** | Medium | Centralized plugin repository |
| **Mobile App** | Low | Native iOS/Android apps |
| **Cloud Service** | Low | Hosted PrintFlowLite as a service |
| **Advanced Analytics** | Low | ML-based print behavior analytics |
| **Print Queue Management UI** | Low | Real-time job monitoring dashboard |

---

## Contributing

PrintFlowLite is a community-driven project. Contributions are welcome.

### How to Contribute

1. **Report Issues** — Submit bug reports and feature requests via GitHub Issues
2. **Documentation** — Improve docs, fix typos, add translations
3. **Code** — Submit pull requests for bug fixes or features
4. **Testing** — Test releases and report findings
5. **Community** — Help answer questions in the community

### Development Priorities

The project is currently focused on:

1. **Stability** — Ensure the fork builds and runs reliably
2. **Modernization** — Update to current Java versions and dependencies
3. **UI Refresh** — Modernize the web interface
4. **DevOps** — Docker, CI/CD, and deployment tooling

---

## Feature Requests

Feature requests can be submitted via [GitHub Issues](https://github.com/YOUR_GITHUB/PrintFlowLite/issues).

When requesting features, please include:

- **Use case** — What problem does this solve?
- **Current workaround** — How is this handled today?
- **Priority** — How important is this feature?
- **Implementation ideas** — Any thoughts on how to approach it?

---

## Community

- **Community Forum**: [wiki.printflowlite.org](https://wiki.printflowlite.org)
- **GitHub Issues**: [github.com/YOUR_GITHUB/PrintFlowLite/issues](https://github.com/YOUR_GITHUB/PrintFlowLite/issues)
- **Email**: info@printflowlite.local

---

## Licensing Note

PrintFlowLite is licensed under the **GNU Affero General Public License (AGPL) v3**.

As a derivative work of SavaPage:
- All original AGPL terms apply
- Source code modifications must be published
- Changes to the code must be documented
- If you run a modified version as a network service, users must be able to access the source

See the LICENSE files in each package for details.

---

## Acknowledgments

PrintFlowLite is based on **SavaPage** by Datraverse B.V. We are grateful to the original developers and community for building a solid foundation.

The SavaPage User Manual (which this documentation is adapted from) was authored by Rijk Ravestein, licensed under Creative Commons Attribution-ShareAlike 4.0.
