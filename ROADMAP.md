# Roadmap

Ordered by value. Items here are **not** present in the product today —
nothing on this list is shown as functional in the UI (§43).

## Next

1. **Payment provider integration** — real PSP (Stripe-class) with verified
   webhooks; until then billing stays admin-managed, by design.
2. **CI security scanning actions** — Trivy/OWASP dependency-check jobs in the
   workflow (the pipeline currently runs secret scan + zero-dummy audit + the
   dedicated security test suite; adding external scanners risks false-red
   builds until baselined).
3. **Docker image build in CI** — `docker build` + compose smoke test
   (requires Docker-in-CI validation; artifacts are already written).
4. **E2E in a browser** — Playwright pass over the UI flows that `e2e.sh`
   currently covers over HTTP.

## Later

5. **Object-lock / WORM retention** for compliance tiers.
6. **S3/MinIO StorageProvider** alongside `LOCAL_FS` (provider interface already
   isolates the seam).
7. **Cross-location redundancy** — copy-on-write second replica per pool.
8. **Webhook/event bus** for external automations (share events, job events).
9. **Per-file virus scanning hook** (ClamAV integration point in the upload
   pipeline).
10. **Localization** of the UI.

## Non-goals

- Simulated payments, fake metrics or any placeholder functionality.
- Automatic drive formatting/partitioning — storage stays administrator-
  registered and explicitly mounted.
