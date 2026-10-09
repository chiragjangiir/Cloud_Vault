# Changelog

All notable changes to Cloud Vault. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/).

## [Unreleased] — 1.0.0

### Added — platform

- **Real storage engine**: streaming uploads with SHA-256 computed during the
  write, generated storage keys (`objects/xx/yy/<rand>.blob`), verified
  streaming downloads with HTTP `Range` support, physical-storage verification.
- **PostgreSQL + Flyway**: `V1__init.sql` (full schema), `V2__seed_plans.sql`
  (plan catalog); Hibernate in `validate` mode — no auto schema generation.
- **Authentication & security**: Spring Security, BCrypt(12), real sessions
  (tracking, revocation, concurrent limits), password change/reset, CSRF cookie
  flow, Postgres-backed rate limiting (login, registration, reset, shares,
  uploads) with externalized limits.
- **Quota engine**: reserve-before-write with atomic DB reservations,
  concurrent-upload safety, 507/413 enforcement, plan `maxFileBytes`,
  downgrade without data loss, admin per-user overrides.
- **Subscriptions**: FREE/BASIC/PRO/BUSINESS plans with real lifecycle states,
  admin-assigned billing, entitlements enforced server-side.
- **Files & folders**: nested folders with breadcrumbs and cycle guard,
  rename/move, trash → restore → permanent purge with physical deletion,
  background trash cleanup with retention.
- **Sharing**: `USER_SHARE` + `PUBLIC_LINK`, random tokens, server-enforced
  expiry, immediate revocation, anonymous view/download endpoints.
- **Search**: database-backed queries by name, extension, MIME, folder, date
  range and size, paginated.
- **Storage pools & locations**: admin-registered locations with real capacity
  and filesystem info, health probes (read/write/capacity) with ONLINE/OFFLINE
  transitions, pool capacity computed from members.
- **Storage migration**: preview → copy → checksum verify → metadata switch →
  source delete, with real progress counters and failure-safe semantics.
- **Reconciliation**: storage audit reporting DB objects vs. physical files,
  orphans and missing objects; quota recompute endpoint.
- **Background jobs**: persistent job table with QUEUED/RUNNING/COMPLETED/
  FAILED/CANCELLED, timestamps, error messages and progress; admin API/UI.
- **Notifications & audit**: event-driven notifications (share expired,
  migration completed, …), searchable audit log with actor/IP/details.
- **API**: REST under `/api/v1` with OpenAPI (springdoc) at `/v3/api-docs`;
  consistent problem-JSON error codes.
- **UI**: responsive light/dark/system Thymeleaf UI — dashboard, browser,
  search, trash, shares, subscription, settings, notifications, admin
  (overview/users/storage/jobs/audit) — every metric from real queries.
- **Data export**: real ZIP (`manifest.json` + file bytes) at
  `GET /api/v1/me/export`.
- **Observability**: Actuator health (app/db/storage) with public
  `/actuator/health`, Prometheus metrics (admin).
- **Docker**: multi-stage non-root `Dockerfile`, `docker-compose.yml` with
  PostgreSQL and mounted `STORAGE_ROOT`, `.env.example`.
- **CI**: GitHub Actions — compile, unit + integration tests on real
  PostgreSQL, package, boot the live JAR, run the 102-check E2E suite, secret
  scan, zero-dummy audit.
- **Tests**: 29 automated tests (unit + integration) covering IDOR, path
  traversal, rate limiting, quota race, sharing, trash, migration, export;
  `scripts/e2e.sh` end-to-end acceptance suite (102 checks).
- **Docs**: README + `docs/` (architecture, storage, drive setup,
  subscriptions, security, deployment, backup, development, API,
  troubleshooting), CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, ROADMAP.

### Fixed

- Migration pagination skipped objects when a location held more than one page
  (re-queries the source set instead of incrementing the page).
- Data export failed inside its read-only transaction (audit write moved to
  the controller after streaming).
- `passwordHash` could serialize into API responses that embed `User`
  (now `@JsonIgnore` + tests asserting no hash/`passwordHash` in responses).
- Rate-limit keys for registration/reset were hardcoded; all limits are now
  externalized (`app.rate-limit.*`).

### Security

- Password hashes excluded from serialization; CI secret scan; no secrets or
  share tokens in logs; traversal/NUL/control-character filename rejection;
  404-without-existence-leak on foreign resources.
