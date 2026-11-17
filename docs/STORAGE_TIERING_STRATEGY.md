# CloudVault Storage Tiering Strategy

## Tiering Matrix
1. **HOT**: Immediate access, local NVMe/SSD cache + S3 Standard.
2. **WARM**: Infrequent access, S3 Standard-IA / MinIO Cold pool (transition after 30 days).
3. **COLD**: Glacier Flexible Retrieval (transition after 90 days).
4. **ARCHIVE**: Deep Archive (transition after 365 days).
