# API reference

Base URL: `/api/v1`. All endpoints are session + CSRF protected except the
public anonymous surfaces listed below. Errors use problem-style JSON:

```json
{ "title": "QUOTA_EXCEEDED", "status": 507, "detail": "Quota exceeded: …" }
```

Machine-readable OpenAPI is served by springdoc:

- `GET /v3/api-docs` — OpenAPI 3 JSON (admin session required)
- `GET /swagger-ui.html` — interactive UI (admin session required)

## Authentication

| Method & path | Purpose | Status codes |
| --- | --- | --- |
| `POST /auth/register` | create account | 201, 409 duplicate, 422 weak, 429 rate |
| `POST /auth/login` | session login | 200 (UserView), 401, 429 |
| `POST /auth/logout` | end session | 204 |
| `GET /auth/me` | current user + plan/quota | 200, 401 |
| `POST /auth/password/change` | change password | 204, 401, 422 |
| `POST /auth/password/reset-request` | request reset token | 202, 429, 503 mail unconfigured |
| `POST /auth/password/reset` | consume reset token | 204, 422 |

CSRF: send the `XSRF-TOKEN` cookie value in the `X-XSRF-TOKEN` header on every
state-changing request.

## Files & folders

| Method & path | Purpose |
| --- | --- |
| `POST /files?name=&folderId=` | streaming upload (`application/octet-stream`, `Content-Length` required) → 201 with `checksum`, `size` |
| `POST /files/upload` | multipart fallback (spooled to disk) |
| `GET /files?folderId=` | browse: current folder, breadcrumbs, folders, files |
| `GET /files/search?q=&ext=&mime=&folderId=&from=&to=&minSize=&maxSize=&page=&size=` | DB-backed search, paged |
| `GET /files/recent` | latest 50 files |
| `GET /files/{id}` / `GET /files/{id}/download` | metadata / streamed bytes, `Range` → 206 |
| `PUT /files/{id}` | rename `{"name": …}` |
| `POST /files/{id}/move` | move `{"folderId": …}` |
| `DELETE /files/{id}` → `POST /files/{id}/restore` → `DELETE /files/{id}/permanent` | trash → restore → purge |
| `GET /files/{id}/versions`, `POST /files/{id}/versions/{n}/activate` | version list / activate |
| `POST/DELETE/PATCH /folders[...]`, `POST /folders/{id}/move` | folder CRUD with cycle guard |

## Shares

| Method & path | Purpose |
| --- | --- |
| `POST /shares` | create `{"fileId","type":"PUBLIC_LINK|USER_SHARE","permission":"VIEW|DOWNLOAD|EDIT","recipient"?,"expiresAt"?}` → 201 with `token` |
| `GET /shares` / `GET /shares/incoming` | my shares / shares for me |
| `DELETE /shares/{id}` | revoke (immediate) |
| `GET /public/shares/{token}` (+`/download`, `/upload`) | anonymous access; 404 unknown, 403 revoked/expired |

## Me (per-user)

`GET /me/overview`, `/me/usage`, `/me/subscription`, `/me/notifications`,
`POST /me/notifications/read-all|{id}/read`, `GET|DELETE /me/sessions[...]`,
`GET /me/export` (real ZIP: `manifest.json` + `data/<folder>/<file>`).

## Admin (ROLE_ADMIN)

`GET /admin/overview`, `GET|PATCH /admin/users`, `POST /admin/users/{id}/plan|password-reset`,
`DELETE /admin/users/{id}/sessions`, `GET|PUT /admin/plans[...]`,
`GET /admin/subscriptions`, `GET|POST /admin/storage/locations[...]`,
`POST /admin/storage/locations/{id}/health|disable`,
`GET /admin/storage/pools`, `GET /admin/storage/migration/preview`,
`POST /admin/storage/migrations`, `POST /admin/storage/audit|verify|quota-reconcile`,
`GET /admin/jobs[/{id}]`, `POST /admin/jobs/{id}/cancel`, `GET /admin/audit`.

## Status code map (§47)

| Code | When |
| --- | --- |
| 401 | not authenticated |
| 403 | authenticated but forbidden (roles, revoked share) |
| 404 | resource not found **or** not yours (no existence leak) |
| 409 | conflict (duplicate username/folder/file) |
| 413 | exceeds plan `maxFileBytes` |
| 422 | validation/sanitization failure |
| 429 | rate limited |
| 507 | quota exceeded |
| 503 | dependency unavailable (storage offline, mail unconfigured) |
