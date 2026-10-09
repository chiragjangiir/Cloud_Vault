# Cloud Vault

**A real, production-grade storage-as-a-service platform.** Every number on
screen comes from the database, every file exists as real bytes under the
configured storage root, and every permission is enforced server-side.

Built with **Java 17 · Spring Boot 3.5 · PostgreSQL · Flyway · Thymeleaf + REST**.

> **Zero-dummy policy.** No mock users, no fake charts, no simulated progress,
> no "coming soon" buttons. If a feature cannot be implemented completely it is
> not shown in the UI. This is enforced by an automated audit in CI (§74).

---

## What actually works

| Area | Behavior |
| --- | --- |
| Authentication | Spring Security, BCrypt (strength 12), sessions, password change/reset, rate-limited login |
| Authorization | Every endpoint requires authentication **and** ownership/role checks; IDOR-tested |
| Storage | Real filesystem under `STORAGE_ROOT`; generated storage keys (never user filenames); SHA-256 verified on write |
| Quotas | Reserver-before-write quota engine with DB-backed reservations; concurrent uploads race-tested |
| Subscriptions | FREE/BASIC/PRO/BUSINESS with real state; admin-assigned; backend enforces limits (size, sharing, versioning) |
| Files & folders | Streaming upload/download, HTTP range, nested folders, rename, move, breadcrumbs, cycle guard |
| Trash | Soft delete with retention, restore, permanent purge that removes the physical object |
| Sharing | `USER_SHARE` + `PUBLIC_LINK`, random tokens, revocation and expiry enforced server-side |
| Search | Real SQL over name/extension/MIME/folder/date/size, paginated |
| Storage pools | Capacity computed from registered locations; placement picks the healthiest location |
| Migration | Admin workflow: preview → copy → checksum verify → metadata switch → source delete, with real progress |
| Health & reconciliation | Real read/write/capacity probes; storage audit reports DB objects vs. physical files vs. orphans |
| Background jobs | Queued→running→completed/failed job table driving trash cleanup, health checks, migration, audits |
| Notifications | Emitted by real events (share expired, migration completed, …) |
| API | REST under `/api/v1/...`, OpenAPI via springdoc (`/v3/api-docs`, `/swagger-ui.html`, admin-only) |
| Observability | Spring Boot Actuator (`/actuator/health` public; metrics/prometheus admin-only) |

## Quick start (local)

Prerequisites: **JDK 17+**, **Maven 3.9+**, **PostgreSQL 14+**.

```bash
# 1. databases (once)
psql -d postgres -c "CREATE ROLE cloudvault LOGIN PASSWORD 'cloudvault'"
psql -d postgres -c "CREATE DATABASE cloudvault OWNER cloudvault"
psql -d postgres -c "CREATE DATABASE cloudvault_test OWNER cloudvault"
psql -d cloudvault_test -c "ALTER SCHEMA public OWNER TO cloudvault"   # PG < 15 only

# 2. run (Flyway migrates automatically on first start)
cd Cloud_Vault
STORAGE_ROOT="$PWD/data/storage" \
ADMIN_USERNAME=admin ADMIN_PASSWORD='Admin!Pass123' \
DB_URL=jdbc:postgresql://localhost:5432/cloudvault \
DB_USER=cloudvault DB_PASSWORD=cloudvault \
mvn spring-boot:run
```

Open <http://localhost:8080> — log in with the admin credentials you passed in.

> The administrator account is **only ever** created from `ADMIN_USERNAME` /
> `ADMIN_PASSWORD`; nothing is hardcoded.

## Quick start (Docker)

```bash
cp .env.example .env      # edit the two change-me passwords
docker compose up -d --build
```

PostgreSQL runs as a service; the app mounts `${STORAGE_HOST_PATH}` (default
`./data/storage`) to `/data/storage`. See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)
and [docs/DRIVE_SETUP.md](docs/DRIVE_SETUP.md) for external-drive setups.

## Tests

```bash
# Unit + integration tests (29 tests). Needs the empty cloudvault_test DB:
# every run drops/recreates its schema, runs REAL Flyway migrations, then
# exercises the real API with MockMvc against real Postgres + a temp storage root.
mvn test

# End-to-end acceptance suite (102 checks) against a LIVE server:
# register → folders → upload real bytes → physical checksum verification →
# download/range → search → shares → IDOR → trash/restore/purge → quota race →
# admin workflows → rate limiting → UI page sweep.
STORAGE_ROOT="$PWD/data/storage" DB_URL=jdbc:postgresql://localhost:5432/cloudvault \
BASE_URL=http://localhost:8080 bash scripts/e2e.sh
```

The suite is **rerunnable**: it resets its own fixtures (test users,
rate-limit windows) before running and never weakens an assertion.

## Repository layout

```
src/main/java/com/cloudvault/
  config/        Security, properties, startup bootstrap
  domain/        JPA entities (User, Plan, Subscription, File, Folder, Share, …)
  repository/    Spring Data JPA repositories
  service/       Business logic: quota, storage, migration, jobs, export, …
  web/api/       REST controllers (/api/v1/...)
  web/ui/        Thymeleaf page controllers (/app, /admin, /share)
  security/      Authentication provider, session enforcement
src/main/resources/
  db/migration/  Flyway V1__init.sql, V2__seed_plans.sql
  templates/     Thymeleaf views (light/dark/system themes)
src/test/        unit/ (pure) + it/ (Spring integration) test suites
scripts/e2e.sh   end-to-end acceptance suite (§56)
docs/            architecture, storage, drive setup, security, deployment, …
```

## Documentation

| Document | Contents |
| --- | --- |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, modules, request flow, transactions |
| [docs/STORAGE.md](docs/STORAGE.md) | StorageProvider, keys, pools, placement, reconciliation |
| [docs/DRIVE_SETUP.md](docs/DRIVE_SETUP.md) | Linux / macOS / Windows external-drive setup |
| [docs/SUBSCRIPTIONS.md](docs/SUBSCRIPTIONS.md) | Plans, quotas, entitlements, downgrade rules |
| [docs/SECURITY.md](docs/SECURITY.md) | Threat model and enforced controls |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | Docker and bare-metal deployment |
| [docs/BACKUP.md](docs/BACKUP.md) | Database + storage backups, restore, DR |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Dev setup, test suite, conventions |
| [docs/API.md](docs/API.md) | REST endpoint reference + OpenAPI |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Common failures and fixes |
| [SECURITY.md](SECURITY.md) | Vulnerability reporting policy |
| [ROADMAP.md](ROADMAP.md) / [CHANGELOG.md](CHANGELOG.md) | Future work / history |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
CI (`.github/workflows/ci.yml`) must be green: compile → tests → package →
live end-to-end suite → secret scan → zero-dummy audit.
