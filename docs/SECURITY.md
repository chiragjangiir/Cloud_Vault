# Security architecture

Threat model and the controls actually enforced. Reporting policy lives in
[../SECURITY.md](../SECURITY.md).

## Authentication (§34)

- Spring Security with a custom `AuthenticationProvider` against the database.
- **BCrypt strength 12**; hashes are written only via `PasswordEncoder` and are
  `@JsonIgnore`d — no API response can ever contain a hash (tested).
- Registration validates username/email/password strength server-side (422).
- Password change requires the current password; password reset uses
  single-use expiring tokens and reports 503 when SMTP is not configured
  (never a fake "email sent").
- Admin accounts are created **only** from `ADMIN_USERNAME`/`ADMIN_PASSWORD`
  environment variables — never hardcoded.

## Sessions (§34)

- Server-side sessions, fixation protection, concurrent-session cap (10),
  tracking with IP/user-agent/last-seen in the settings page.
- Sessions are revocable (self-service and admin), invalidated on logout and
  on password change; `SessionEnforcementFilter` revalidates session state on
  every request.

## Authorization (§35)

- `anyRequest().authenticated()` by default; `/admin/**` and admin APIs require
  `ROLE_ADMIN`; `/api/v1/public/**` and auth endpoints are the only anonymous
  surfaces.
- Ownership checks live in the service layer (`getOwned` style): a foreign
  resource returns **404 without an existence leak** — IDOR-tested for
  read/download/delete/purge.
- API clients get JSON problem responses (401/403); browsers get redirect/403.

## Input safety (§8, §36)

- `FolderService.sanitizeName` rejects separators, `..`, `.`, NUL bytes,
  control characters and over-length names with 422 — unit tested per attack
  string. Physical paths are always generated (`objects/xx/yy/<rand>.blob`).
- Uploads require a declared `Content-Length`; size is capped by the plan
  (`maxFileBytes` → 413) and by servlet config; content type is recorded, not
  trusted for authorization decisions.

## CSRF (§36)

`CookieCsrfTokenRepository` (readable cookie for the JS client) with a raw
pass-through token handler; every state-changing request must echo the token.
Integration tests exercise the same cookie/header flow the browser uses.

## Rate limiting (§48)

Fixed-window counters stored in Postgres (`rate_limit_counters`, single atomic
upsert — no race bypass): login (per IP+username), API login, registration,
password reset, share creation, share view/download, uploads. Limits are
externalized via `app.rate-limit.*` environment-overridable properties.
Exhaustion returns **429** with `Retry` guidance.

## Data exposure (§62)

- No passwords, hashes, share tokens, session ids or reset tokens in logs —
  share tokens are explicitly excluded from audit details (see `ShareService`).
- `passwordHash` is `@JsonIgnore`d; CI greps for credential material and
  private keys; tests assert no `passwordHash`/bcrypt prefix appears in admin
  or user API responses.

## Testing security (§36)

`AuthSecurityTest` covers IDOR, admin-endpoint denial, anonymous access,
rate limiting, password change and credential exposure;
`FilenameSanitizationTest` covers traversal strings;
`SharingTest` covers token guessing, revocation and expiry;
`QuotaConcurrencyTest` covers quota bypass attempts; the E2E suite repeats the
critical paths against a live server.
