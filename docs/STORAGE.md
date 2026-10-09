# Storage

## Principles

1. **Real bytes, real place.** An upload only succeeds after the bytes are
   written under the configured storage root and the checksum matches.
2. **Keys are generated, never user input.** The physical path is
   `STORAGE_ROOT/objects/xx/yy/<random>.blob`; user filenames live only in
   the database. Path traversal is structurally impossible (§8).
3. **Metadata and bytes are reconciled**, never assumed.

## Entities

| Entity | Meaning |
| --- | --- |
| `StorageLocation` | An administrator-registered directory: name, root path, provider (`LOCAL_FS`), status (`ONLINE`/`OFFLINE`/`DISABLED`), real capacity, filesystem type, last health check + message |
| `StoragePool` | Named group (`MAIN_STORAGE` by default) whose capacity is the sum of its member locations — never hardcoded |
| `StorageObject` | One physical blob: `storageKey`, `physicalReference` (absolute path resolved at write time), size, SHA-256, content type, owning location |
| `FileVersion` | A version row pointing at a `StorageObject`; the file's `active_version_id` decides which one serves downloads |

## Upload → physical write

```
POST /api/v1/files?name=photo.jpg   (application/octet-stream)
  → sanitize name → reserve quota (atomic) → pick placement
  → provider.write(): stream to <root>/objects/ab/cd/<rand>.blob while hashing
  → SHA-256 computed during the write (no second pass over the data)
  → TX: storage_objects + file_versions + files + usage + reservation release
  → failure at any step: delete partial object, release reservation, report error
```

Downloads stream from the verified object; `Range` requests are honoured
(`206 Partial Content`) for resume and media playback. If the hosting location
is OFFLINE the read fails loudly with *storage unavailable* — files never
pretend to work.

## Placement & pools

`StoragePoolService.pick(bytes)` iterates the pool's locations and chooses an
ONLINE, writable location with enough free space — preferring the one with the
most free capacity. Pool totals shown in the admin UI are computed from the
registered locations (`totalCapacityBytes`, `usedBytes`, `freeBytes`).

## Health checks

`StorageLocationService.healthCheck(id)` actually verifies the location:

- path exists and is a directory,
- readable **and** writable (creates/removes a probe file),
- usable capacity ≥ configured minimum,
- records `lastHealthCheck` + `lastHealthMessage` and flips status to
  `OFFLINE` when verification fails (e.g., the drive was unplugged).

A location that comes back and passes the probe returns to `ONLINE`.

## Reconciliation / audit

`POST /api/v1/admin/storage/audit` runs a real scan and reports:

- database objects vs. physical files,
- **orphans** (blob without a DB row),
- **missing** (DB row without bytes),
- checksum verification (via `storage/verify`).

Results come from actual enumeration — nothing is fabricated, and the audit
never silently "fixes" data.

## Migration (storage → storage)

```
preview  GET  /api/v1/admin/storage/migration/preview?sourceId=&destinationId=
start    POST /api/v1/admin/storage/migrations {"sourceId","destinationId"}
job      GET  /api/v1/admin/jobs/{id}   → progress from real counters
```

Per object: reserve nothing at the source → copy to destination → compare
SHA-256 → switch metadata (own transaction) → delete source. Any failure aborts
with the **source left intact**. Progress is `processed/total` objects and
bytes actually moved.

## Trash & deletion

- Normal delete: row marked `TRASHED` (`deletedAt`, original folder, retention) —
  bytes stay so restore is lossless.
- Restore: status back to `ACTIVE`, downloadable again.
- Permanent delete: physical object deleted; the test suite asserts the blob is
  gone from disk.
- `TRASH_CLEANUP` job purges rows past their retention window.

## Quota accounting

`user_usage.used_bytes` is a hot-path counter updated inside the upload
transaction; `quota_reservations` hold in-flight capacity so two simultaneous
uploads cannot both win the last gigabyte. The admin endpoint
`/api/v1/admin/storage/quota-reconcile` recomputes usage from
`storage_objects` to correct drift.
