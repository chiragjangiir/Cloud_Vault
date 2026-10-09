# Subscriptions & quotas

## Plans

Plans are database rows (seeded by `V2__seed_plans.sql`, fully editable by
admins). Exact seeded values:

| Plan | Storage | Max file | Sharing | Versioning | Max versions | Retention |
| --- | --- | --- | --- | --- | --- | --- |
| FREE | 2 GiB | 100 MiB | ✓ | ✕ | 1 | 7 days |
| BASIC | 50 GiB | 1 GiB | ✓ | ✕ | 3 | 14 days |
| PRO | 500 GiB | 10 GiB | ✓ | ✓ | 10 | 30 days |
| BUSINESS | 2 TiB | 50 GiB | ✓ | ✓ | 100 | 90 days |

Admins can change any field at runtime (`PUT /api/v1/admin/plans/{id}`);
changes persist and take effect **immediately** for backend authorization —
there is no frontend entitlement.

## Subscription state

`subscriptions.status` carries the real lifecycle:
`ACTIVE`, `TRIALING`, `PAST_DUE`, `CANCELLED`, `EXPIRED`, `SUSPENDED`.

`SubscriptionService.limitsFor(user)` is the single source of truth for
entitlements:

- effective quota = plan `storage_bytes`, **unless** the subscription carries
  an admin `storage_bytes_override`,
- `maxFileBytes`, sharing enabled, versioning + max versions, API access,
  retention days, optional daily download caps,
- `entitled` = subscription exists, is in force, and the account is active.

Suspended/expired accounts are rejected by `requireNotSuspended` before any
upload — server-side, regardless of what the UI shows.

## Billing model

Initial model is **admin-managed billing** (§11): administrators assign a plan
and status to a real user:

```
POST /api/v1/admin/users/{id}/plan
{ "planCode": "PRO", "status": "ACTIVE", "storageOverride": null, "note": "…" }
```

No fake "payment successful" UI exists anywhere; payment-provider integration
(webhook-verified) is deliberately a roadmap item, not a simulation.

## Quota enforcement (§12, §13)

1. Upload requests hit `QuotaService.reserve(user, size, reason)` **before any
   bytes are written**.
2. `reserve` runs as a single atomic SQL operation against
   `user_usage` + `quota_reservations`, so two concurrent uploads that
   together exceed the quota cannot both win — one commits, the other gets
   **507 Insufficient Storage**. This is covered by a dedicated concurrency
   test.
3. Reservations expire (janitor releases stale rows from crashed uploads).
4. `maxFileBytes` is checked in the same step → **413**.

## Downgrade rule (§32)

Lowering a user's quota **never deletes files**:

- usage > quota ⇒ account is `OVER_QUOTA`: new uploads rejected (507),
- existing files remain downloadable, deletable and exportable,
- once usage fits again (user deletes data, or admin raises the quota),
  uploads resume automatically.

## Admin overrides (§33)

`storageOverride` on a subscription overrides the plan quota per user; plan
edits (storage, max file size, sharing/versioning flags, retention) apply to
every subscriber of that plan. Both are covered by integration tests
(`QuotaConcurrencyTest`, `AdminExportMigrationTest`).
