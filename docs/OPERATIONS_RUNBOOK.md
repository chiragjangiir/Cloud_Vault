# CloudVault Production Operations & Disaster Recovery Runbook

## Backup Verification
- MinIO and S3 bucket replication status check via Prometheus alerts.
- PostgreSQL metadata consistent snapshots every 6 hours with point-in-time recovery.
