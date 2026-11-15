# CloudVault Database Schema Reference

This document describes the PostgreSQL relational schema, key entities, and indexing strategies used by CloudVault.

---

## 1. Core Tables

### `users`
Stores user identities, hashed credentials, roles, and quota usage.
- `id` (BIGINT PRIMARY KEY)
- `username` (VARCHAR UNIQUE NOT NULL)
- `email` (VARCHAR UNIQUE NOT NULL)
- `password_hash` (VARCHAR NOT NULL)
- `role` (VARCHAR NOT NULL)
- `quota_bytes` (BIGINT NOT NULL)

### `file_entries`
Metadata for user files, folder hierarchy, and logical pointers to object blobs.
- `id` (BIGINT PRIMARY KEY)
- `owner_id` (BIGINT REFERENCES users(id))
- `folder_id` (BIGINT REFERENCES folders(id))
- `filename` (VARCHAR NOT NULL)
- `size_bytes` (BIGINT NOT NULL)
- `sha256_checksum` (VARCHAR(64) NOT NULL)
- `created_at` (TIMESTAMP WITH TIME ZONE NOT NULL)

---

## 2. Referential Integrity
- All state-changing cascades follow soft-delete or orphan-preventing foreign keys.
- Unique constraints enforce per-folder filename collision avoidance.
