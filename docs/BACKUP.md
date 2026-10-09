# Backup & disaster recovery

> **A second drive is not a backup** (§63). Migration/replication protects
> against drive failure; only an independent, versioned copy protects against
> deletion, corruption and ransomware.

## What to back up

| Asset | Why | Method |
| --- | --- | --- |
| PostgreSQL database | all metadata, shares, quotas, audit | `pg_dump` |
| Storage blobs (`STORAGE_ROOT`) | the actual files | file-level copy (rsync/restic/borg) |
| `.env` / configuration | database credentials, admin bootstrap, limits | copy of the file (secret manager) |

Database and blobs must be **consistent with each other**: take the DB dump
first, then snapshot the files (files created after the dump are harmless
orphans the storage audit reports; files deleted after the dump show as
"missing" and can be re-uploaded or pruned).

## Backup commands

```bash
# Database (compressed custom format)
pg_dump -Fc -d cloudvault -f "cloudvault-$(date +%F-%H%M).dump"

# Storage blobs (example: restic to a remote repo — off this machine!)
restic -r /mnt/backup-repos/cloudvault backup "$STORAGE_ROOT"
```

Example daily cron (unprivileged user with access to both):

```cron
15 2 * * * pg_dump -Fc -d cloudvault -f /mnt/backups/cloudvault-$(date +\%F).dump
30 2 * * * restic -r /mnt/backup-repos/cloudvault backup /srv/cloudvault/storage --tag daily
45 2 * * * restic -r /mnt/backup-repos/cloudvault forget --keep-daily 14 --keep-weekly 8 --keep-monthly 12
```

## Restore

```bash
# 1. stop the application
# 2. restore the database
pg_restore -d cloudvault --clean --if-exists cloudvault-2026-10-09.dump
# 3. restore blobs
restic -r /mnt/backup-repos/cloudvault restore latest --target /mnt/cloudvault-storage
# 4. start the application, then run a storage audit:
#    Admin → Storage → Run audit  (expect 0 missing; orphans are listed, never auto-deleted)
```

## Disaster-recovery scenarios (§64)

| Scenario | Recovery |
| --- | --- |
| **Database loss** | Restore dump; blobs are intact. Shares/quotas/users return as of the dump time. Run storage audit. |
| **Storage location loss** (drive died) | Restore blobs from backup into a new location; register it; run audit. If only *some* files are lost, the audit's "missing" list is your work list — restore those objects, then re-check. |
| **Application restart / corruption** | Redeploy; Flyway is idempotent; state lives in DB + storage, not in the process. |
| **Drive reconnection** | Health check → `ONLINE` (no data movement needed if the mount is unchanged). |
| **Partial migration failure** | Source was left intact by design; inspect the job's `errorMessage`, fix the destination (space/permissions), re-run the migration. |
| **Accidental deletion** | Restore DB dump + blobs from before the deletion (versioned repo). Trash items within their retention window need no restore. |

## What is *not* a backup

- A second mounted drive kept in the same machine (failure domain shared).
- The migration feature (it moves data, doesn't version it).
- The database alone, without blobs (metadata without files).
