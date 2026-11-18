# CloudVault Object Lifecycle Automation Specification

## 1. Overview
The Object Lifecycle Management engine governs transition rules, expiration, cold archive migration, and compliance legal holds across CloudVault object storage containers.

## 2. Storage Tiering Ladder
Objects follow a monotonically cooling transition path:
1. **HOT**: Default tier for high-frequency access.
2. **WARM**: Infrequent Access (IA), 30-day minimum retention.
3. **COLD**: Archive storage, 90-day minimum retention.
4. **DEEP_ARCHIVE**: Tape-equivalent glacier storage, 180-day minimum retention.

## 3. Compliance and Legal Hold
When `LegalHold` is set to `ON`, all delete and overwrite operations return HTTP 403 `AccessDenied` regardless of caller permissions until explicit administrative release.
