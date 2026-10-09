# Security Policy

## Reporting a vulnerability

Please report security issues **privately** using GitHub's advisory form:

<https://github.com/chiragjangiir/Cloud_Vault/security/advisories/new>

Please do **not** open a public issue for vulnerabilities. You can expect:

- **Acknowledgement** within 3 working days.
- An initial assessment within 10 working days.
- A fix or mitigation plan agreed with you before any public disclosure.
- Credit in `CHANGELOG.md` unless you prefer to remain anonymous.

## Supported versions

Only the latest release on `main` receives security updates.

| Version | Supported |
| --- | --- |
| latest `main` | ✅ |
| older commits / forks | ❌ |

## What is enforced (and tested)

- **Passwords** — BCrypt (strength 12); hashes are `@JsonIgnore`d and never
  appear in any API response (asserted by `AuthSecurityTest`).
- **Sessions** — server-side sessions with fixation protection, concurrent
  session limits, revocation from the settings page, and invalidation on
  logout/password change.
- **CSRF** — `CookieCsrfTokenRepository` on every state-changing request.
- **Authorization** — authentication *and* ownership/role checks on every
  protected resource; cross-user access returns 404 without existence leaks;
  admin endpoints return 403 for users and 401 for anonymous callers.
- **Path traversal** — filenames are sanitized (separators, `..`, NUL, control
  characters rejected with 422); physical paths use generated storage keys,
  never user input.
- **Rate limiting** — fixed-window Postgres-backed counters on login,
  registration, password reset, shares and uploads.
- **Share links** — cryptographically random tokens, server-enforced expiry
  and immediate revocation.
- **Transport/data hygiene** — secrets come from environment variables only;
  no credentials, tokens, share links or password hashes are ever logged
  (enforced by code review + CI secret scan).

## Scope

In scope: the application code in this repository, its default configuration
and the provided Docker setup. Out of scope: vulnerabilities in third-party
dependencies already fixed upstream, social engineering, and attacks requiring
root access to the host running Cloud Vault.
