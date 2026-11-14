# CloudVault REST API Specification

This specification documents the public and authenticated REST API endpoints exposed by CloudVault.

---

## Base URL
`/api`

## Authentication
Authentication is session-cookie or basic-auth driven. State-changing requests (`POST`, `PUT`, `DELETE`) require a CSRF token provided via `X-XSRF-TOKEN` header or form parameter.

---

## 1. Authentication Endpoints

### `POST /api/auth/login`
- **Request Body**: `{"username": "...", "password": "..."}`
- **Response**: `200 OK` on success with session cookie; `401 Unauthorized` on bad credentials.

### `POST /api/auth/logout`
- **Response**: `204 No Content`, invalidates session.

---

## 2. File Operations

### `GET /api/files`
- **Query Params**: `folderId` (optional), `page`, `size`
- **Response**: List of `FileEntry` objects.

### `POST /api/files/upload`
- **Content-Type**: `multipart/form-data`
- **Fields**: `file`, `folderId` (optional)
- **Response**: `201 Created` with file metadata.

### `GET /api/files/{id}/download`
- **Response**: Binary stream with `Content-Disposition` header.

### `DELETE /api/files/{id}`
- **Response**: `204 No Content` (moves to trash or permanently deletes).

---

## 3. Public Sharing

### `GET /share/{token}`
- **Response**: Shared file landing page or direct download stream.
