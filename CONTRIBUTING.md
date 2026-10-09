# Contributing to Cloud Vault

Thanks for helping. This project ships **production functionality only** —
the bar is "would this survive a real user".

## Ground rules

1. **Zero dummy functionality.** No mocks, placeholder responses, hardcoded
   numbers, TODO buttons or "coming soon" features in production code or UI.
   If it can't be finished, don't expose it.
2. **Reality over appearance.** Data safety and security outrank visuals.
3. **Tests ship with features.** A change without coverage for its happy path
   *and* its failure path isn't done.

## Workflow

```bash
git checkout -b feature/your-change
mvn test                     # unit + integration (needs cloudvault_test DB)
mvn -DskipTests package
# start the app, then:
BASE_URL=http://localhost:8080 STORAGE_ROOT="$PWD/data/storage" \
DB_URL=jdbc:postgresql://localhost:5432/cloudvault bash scripts/e2e.sh
```

Open a PR against `main`. CI must be green: compile → tests → package → live
E2E → secret scan → zero-dummy audit.

## Coding guidelines

- Java 17, Spring Boot idioms; business rules in `service/`, never in
  templates or JavaScript.
- REST under `/api/v1`, DTO records — entities with credentials never cross
  the wire.
- Correct status codes via `ApiException` (404 without existence leaks, 507
  quota, 413 size, 409 conflict, 422 validation, 429 rate limit).
- Schema changes = new Flyway version file; **never** edit an applied
  migration.
- Structured, secret-free logs; no `catch (Exception e) {}`.

## Reporting bugs

Use GitHub issues for functional bugs; report security issues privately per
[SECURITY.md](SECURITY.md). Include reproduction steps, expected vs. actual,
and environment (OS, JDK, PostgreSQL version).
