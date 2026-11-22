# Multi-Tenant S3 Object Storage Isolation Specification

## Overview
This document defines the zero-trust isolation boundaries for CloudVault multi-tenant storage clusters.

## Tenant Partitioning
1. **Namespace Isolation**:
   - Every tenant is mapped to an isolated namespace prefix `<tenant_id>/`.
   - Direct physical path traversals (`../`) are filtered and rejected at API gateway.

2. **KMS Key Segregation**:
   - Each tenant possesses an isolated Customer Managed Key (CMK).
   - Envelope encryption decrypts chunks in-memory only.

3. **Rate Limiting & Noisy-Neighbor Quotas**:
   - Per-tenant leaky bucket token limiter for aggregate PUT/GET IOPS.
