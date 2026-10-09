# Development

## Prerequisites

- JDK 17+ (builds also run on 21/23)
- Maven 3.9+
- PostgreSQL 14+ (local or Docker)

## First run

```bash
psql -d postgres -c "CREATE ROLE cloudvault LOGIN PASSWORD 'cloudvault'"
psql -d postgres -c "CREATE DATABASE cloudvault OWNER cloudvault"
psql -d postgres -c "CREATE DATABASE cloudvault_test OWNER cloudvault"
# PostgreSQL < 15 only: let the test role own its schema so the suite can reset it
psql -d cloudvault_test -c "ALTER SCHEMA public OWNER TO cloudvault"

STORAGE_ROOT="$PWD/data/storage" \
ADMIN_USERNAME=admin ADMIN_PASSWORD='Admin!Pass123' \
mvn spring-boot:run
```

Flyway migrates on start (`V1__init.sql`, `V2__seed_plans.sql`); Hibernate runs
in `validate` mode — the schema is never auto-generated.

## Test suite (§53–§56)

```bash
mvn test
```

| Layer | What it does |
| --- | --- |
| `unit/` | Pure JUnit: filename sanitization attacks, formatting math — no Spring context |
| `it/` | `@SpringBootTest` + MockMvc against **real PostgreSQL** (`cloudvault_test`) and a **real temp storage root**; every run drops the test schema so Flyway re-migrates from scratch and no state leaks between runs |

Integration coverage includes: auth/session lifecycle, IDOR, admin denial,
rate limiting, credential exposure, upload→physical→download→range→search,
trash/restore/purge, malicious filenames, plan file-size limits, quota
enforcement, **concurrent upload race (exactly one winner)**, public/user
sharing with revocation and expiry, admin dashboards, plan propagation, ZIP
export, and a **real storage migration job** (copy → checksum → switch → delete
source) with progress assertions.

```bash
# End-to-end acceptance suite against a running server (§56):
STORAGE_ROOT="$PWD/data/storage" DB_URL=jdbc:postgresql://localhost:5432/cloudvault \
BASE_URL=http://localhost:8080 bash scripts/e2e.sh
```

`e2e.sh` resets its own fixtures (test users `alice`/`bob`, rate-limit windows)
so it is rerunnable; it never weakens an assertion. Exit code 0 = all green.

## Conventions

- **No dummy functionality** (§0): no TODO/mock/placeholder paths reachable from
  the UI; CI fails the build on such markers in `src/main`.
- Business rules live in `service/`, never in templates or JS.
- New endpoints: `/api/v1/...` in `web/api`, DTO records (never entities with
  credentials), correct status codes via `ApiException`.
- Schema changes: new Flyway version files only — never edit an applied
  migration (checksums are validated at startup).
- Every feature ships with tests: run `mvn test` + the E2E suite before a PR.

## CI

`.github/workflows/ci.yml`: compile → `mvn test` → package → boot the real JAR →
`scripts/e2e.sh` → secret scan → zero-dummy audit. A green badge means all of
that actually ran.
