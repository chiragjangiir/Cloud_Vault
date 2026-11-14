# CloudVault System Architecture & Execution Flows

This document details the internal architectural components and sequence flows within CloudVault.

---

## 1. High-Level System Architecture

```
                       +-------------------+
                       |    Web Browser    |
                       +---------+---------+
                                 |
                                 | HTTPS (Session Cookie / CSRF)
                                 v
                       +---------+---------+
                       |    Nginx / ALB    |
                       +---------+---------+
                                 |
                                 v
                  +--------------+--------------+
                  |     CloudVault App (8080)   |
                  |  - Spring Security Filter   |
                  |  - SessionEnforcementFilter |
                  |  - StorageLocationService   |
                  |  - QuotaReservationService  |
                  +-------+-------------+-------+
                          |             |
           Metadata / JPA |             | Streaming I/O
                          v             v
                  +-------+-----+  +----+---------------+
                  | PostgreSQL  |  | Filesystem Storage |
                  |  (Metadata) |  |   (/data/storage)  |
                  +-------------+  +--------------------+
```

---

## 2. File Upload & Quota Reservation Sequence

```
Client               FilesApiController       QuotaService      StorageProvider    PostgreSQL
  |                          |                     |                   |               |
  |--- POST /api/files ----->|                     |                   |               |
  |    (multipart)           |-- checkQuota() ---->|                   |               |
  |                          |<-- reserved ok -----|                   |               |
  |                          |-- streamToFile() ---------------------->|               |
  |                          |                                         |-- writeDisk ->|
  |                          |<-- SHA-256 digest + bytes written ------|               |
  |                          |-- saveMetadata() -------------------------------------->|
  |                          |-- commitQuota() --->|                                   |
  |<-- 201 Created ----------|                     |                                   |
```
