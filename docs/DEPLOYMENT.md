# Deployment

## Docker (recommended)

```bash
cp .env.example .env          # set POSTGRES_PASSWORD and ADMIN_PASSWORD
docker compose up -d --build
curl -fsS http://localhost:8080/actuator/health   # {"status":"UP"}
```

What the stack does:

- `postgres:16-alpine` with a health gate (`pg_isready`) and a named volume.
- Cloud Vault built from the multi-stage `Dockerfile` (Maven → JRE 21),
  running as **non-root** uid 10001, health-checked via `/actuator/health`.
- `${STORAGE_HOST_PATH}` (default `./data/storage`, set an external-drive path
  in production) is mounted at `/data/storage` → `STORAGE_ROOT`.
- Flyway migrates the schema automatically on start; the admin account is
  created from `ADMIN_USERNAME`/`ADMIN_PASSWORD`.

## Bare metal

```bash
mvn -DskipTests package
STORAGE_ROOT=/mnt/cloudvault-storage \
DB_URL=jdbc:postgresql://localhost:5432/cloudvault \
DB_USER=cloudvault DB_PASSWORD='…' \
ADMIN_USERNAME=admin ADMIN_PASSWORD='…' \
APP_PUBLIC_URL=https://vault.example.com \
java -jar target/cloud-vault-1.0.0-SNAPSHOT.jar
```

Run it under a service manager as an unprivileged user:

```ini
# /etc/systemd/system/cloudvault.service (excerpt)
[Service]
User=cloudvault
Environment=STORAGE_ROOT=/mnt/cloudvault-storage
Environment=DB_URL=jdbc:postgresql://localhost:5432/cloudvault
ExecStart=/usr/bin/java -Xmx512m -jar /opt/cloudvault/app.jar
Restart=on-failure
```

## Production checklist (§59)

- [ ] Strong `POSTGRES_PASSWORD`, `ADMIN_PASSWORD` (≥10 chars) — only in `.env`
      or your secret manager, never in git (`.env` is ignored; CI enforces).
- [ ] TLS termination in front (reverse proxy); `APP_PUBLIC_URL` = https URL
      so share links are correct.
- [ ] `STORAGE_ROOT` on a dedicated mounted volume with backups (see BACKUP.md).
- [ ] Review rate limits (`RATE_LIMIT_*`) for your user base.
- [ ] `/actuator/health` is public; every other actuator endpoint requires
      admin — restrict at the proxy if metrics should stay internal.
- [ ] Watch logs for `ERROR` entries; job failures surface in
      Admin → Jobs with `errorMessage`.

## Upgrades

1. Backup database + storage (BACKUP.md).
2. Deploy the new image/jar — Flyway applies migrations in order on start.
3. Verify `/actuator/health` and run `scripts/e2e.sh` against staging.

Rollback: restore the previous image **and** the database dump taken before
the upgrade (Flyway migrations are forward-only).
