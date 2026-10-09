# Troubleshooting

## Application won't start

**`Connection refused` / Hikari pool errors**
→ PostgreSQL isn't running or `DB_URL`/`DB_USER`/`DB_PASSWORD` are wrong.
Check: `pg_isready` and `psql -d cloudvault -c 'select1'`.

**`FlywayValidateException: Migration checksum mismatch`**
→ An already-applied migration file was edited. Restore the original file, or
drop the dev database and let it re-migrate (dev only — never in production).

**`Migration already applied but not resolved locally`**
→ Stray migration files; remove them or baseline properly.

**`Storage root … does not exist / is not writable`**
→ Create the directory and grant the service user access:
`mkdir -p "$STORAGE_ROOT" && chown` (see DRIVE_SETUP.md). The app refuses to
boot with an unusable default location — by design.

**Admin account not created**
→ `ADMIN_USERNAME`/`ADMIN_PASSWORD` unset or blank; the log prints
`no administrator account bootstrapped`. Restart with both set.

## Requests fail

**429 on login/register** — fixed-window rate limit. Wait for the window to
roll over or adjust `RATE_LIMIT_LOGIN` / `RATE_LIMIT_REGISTER`.

**403 on `/swagger-ui.html` or `/v3/api-docs`** — OpenAPI is admin-only; log
in as admin first (or call with an admin session cookie).

**507 on upload** — quota reached (or an admin override is below current
usage). Existing files remain downloadable; raise the quota or free space.

**413 on upload** — file larger than the plan's `maxFileBytes`.

**CSRF errors (403 with `CSRF` title)** — missing/rotated `X-XSRF-TOKEN`
header; re-read the cookie (the UI and `scripts/e2e.sh` both do this).

## Storage

**Admin → Storage shows OFFLINE** — the path is gone (unmounted drive).
Reconnect the mount and run the health check. Files on an OFFLINE location
fail with *storage temporarily unavailable* until it returns.

**Audit reports orphans/missing** — orphan = blob without DB row (usually an
interrupted write; safe to remove after confirming with `storage/verify`);
missing = DB row without bytes → restore from backup (BACKUP.md).

**Migration job FAILED** — read `errorMessage` in Admin → Jobs; source data is
intact by design. Typical causes: destination full, destination offline,
permissions.

## Tests

**`Cannot reset test database … create it first`**
→ `psql -d postgres -c "CREATE DATABASE cloudvault_test OWNER cloudvault"`.

**`must be owner of schema public` (PostgreSQL < 15)**
→ `psql -d cloudvault_test -c "ALTER SCHEMA public OWNER TO cloudvault"`.

**E2E fails with 409/429 on a rerun** — run it with `DB_URL` set so the suite
can reset its fixtures; it prints `FIXTURE RESET: …` at startup.

**Port 8080 already in use** — `lsof -i :8080`, stop the other process or set
`SERVER_PORT`.

## Logs

Application logs stream to stdout (and your redirect target). Look for:

- `ERROR … ApiExceptionHandler : API failure code=…` — user-visible failures.
- `ERROR … scheduled task` — background job failures (also visible in
  Admin → Jobs with `errorMessage`).
- `Storage audit finished: N db objects, M physical, … orphans` — reconciliation
  result after each audit run.

Passwords, tokens and share links are never logged (enforced by review + CI).
