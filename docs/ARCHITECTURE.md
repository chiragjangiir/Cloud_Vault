# Architecture

Cloud Vault is a single Spring Boot 3.5 application (Java 17) with a layered,
strictly server-side design. There is no client-side authority: the browser
renders what the server says and every rule is re-checked on the server.

```
Browser (Thymeleaf + app.js)          API clients (REST /api/v1)
        │                                      │
        └──────────────┬───────────────────────┘
                       ▼
        SecurityFilterChain (Spring Security)
          ├─ session enforcement (SessionEnforcementFilter)
          ├─ login rate limiting (fixed window, Postgres counters)
          └─ CSRF (cookie repository, raw pass-through handler)
                       ▼
   web/ui  (page controllers)   web/api  (REST controllers)
                       ▼
                service layer
   ┌────────────┬────────────┬─────────────┬──────────────┐
   │ FileService│ QuotaService│ShareService │MigrationService│
   │ FolderSvc  │Subscription│ExportService│Reconciliation  │
   │ ObjectStorage│Audit/Notify│JobService  │StorageHealth   │
   └────────────┴────────────┴─────────────┴──────────────┘
                       ▼
        repositories (Spring Data JPA) ── PostgreSQL (Flyway-managed)
                       ▼
        StorageProvider → real filesystem under STORAGE_ROOT
```

## Modules

| Package | Responsibility |
| --- | --- |
| `config` | `SecurityConfig`, `AppProperties`, `DataBootstrap` (validates storage, syncs pools, creates admin **only** from env vars) |
| `domain` | JPA entities: `User`, `Plan`, `Subscription`, `Folder`, `FileEntry`, `FileVersion`, `Share`, `StorageLocation`, `StoragePool`, `StorageObject`, `QuotaReservation`, `BackgroundJob`, `AuditLog`, `Notification`, `UserSession`, `RateLimitCounter` |
| `repository` | Spring Data repositories + a few `@Modifying`/native queries for counters |
| `service` | All business rules; transactions live here |
| `web/api` | REST controllers under `/api/v1` returning DTOs |
| `web/ui` | Thymeleaf page controllers for `/app`, `/admin`, `/share` |
| `web/error` | `ApiException` + `ApiExceptionHandler` → RFC7807-style problem JSON with correct status codes |
| `security` | Authentication provider, user details, session tracking/enforcement |

## Request lifecycle (upload example)

1. Authentication → session; CSRF token verified.
2. `FilesApiController.uploadRaw` streams the request body (never buffered whole).
3. `FileService.upload`:
   - subscription state check (`requireNotSuspended`),
   - filename sanitization,
   - **quota reservation** (`QuotaService.reserve`, single atomic SQL upsert —
     also enforces the plan's `maxFileBytes` → 413),
   - placement: `StoragePoolService.pick` chooses the healthiest ONLINE
     location with enough free space,
   - `ObjectStorageService.writePhysical` streams bytes and computes SHA-256,
   - one transaction commits `StorageObject` + `FileEntry`/`FileVersion` +
     usage counters + reservation release,
   - on any failure: physical object deleted, reservation released, the real
     error surfaced (never swallowed).
4. Audit entry + notification are written after the commit.

## Transactions

- Upload metadata commit uses `TransactionTemplate` around a single atomic unit
  so database state can never claim a file that storage does not have.
- Quota reservations are DB rows with expiry (`quota_reservations`), released on
  success/failure and by a scheduled janitor — this is what makes concurrent
  uploads safe.
- Migration updates object metadata in its own transaction **before** deleting
  the source copy; a checksum mismatch aborts with the source intact.
- Flyway is the only schema manager (`ddl-auto=validate`); Hibernate never
  generates schema.

## Background processing

`JobService` owns a `background_jobs` table (QUEUED → RUNNING →
COMPLETED/FAILED/CANCELLED with `startedAt`/`completedAt`/`errorMessage` and
`progressProcessed/progressTotal/bytesProcessed/bytesTotal`). `BackgroundScheduler`
enqueues storage health checks and trash cleanup periodically; expired shares,
stale reservations, rate-limit windows and reset tokens are swept on schedules.
The admin UI and `/api/v1/admin/jobs` show the real rows.

## Error handling

`ApiException` carries an HTTP status + machine code; `ApiExceptionHandler`
maps validation failures (422), conflicts (409), quota (507), payload size
(413), rate limits (429), auth (401) and authorization (403) consistently for
API and UI paths. Scheduled jobs log with full stack traces — nothing is
silently swallowed.
