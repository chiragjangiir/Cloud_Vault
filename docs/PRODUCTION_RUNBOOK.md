# CloudVault Production Operations Runbook

This guide covers operational procedures for deploying, maintaining, and recovering CloudVault in production.

---

## 1. System Requirements & Provisioning

- **Operating System**: Linux (Ubuntu 22.04 LTS / Debian 12 / RHEL 9)
- **Memory**: Minimum 2 GB RAM (4 GB recommended)
- **Storage**: Fast NVMe or SSD storage for metadata database; persistent block storage for object repository.

---

## 2. Nginx Reverse Proxy Configuration

```nginx
server {
    listen 443 ssl http2;
    server_name vault.example.com;

    ssl_certificate /etc/letsencrypt/live/vault.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/vault.example.com/privkey.pem;

    client_max_body_size 10G;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
    }
}
```

---

## 3. Database Backup & Disaster Recovery

```bash
# Automated daily backup of metadata
pg_dump -U cloudvault -h localhost -d cloudvault -Fc -f /backups/cloudvault_$(date +%Y%m%d_%H%M%S).dump

# Restore procedure
pg_restore -U cloudvault -d cloudvault --clean --if-exists /backups/cloudvault_YYYYMMDD_HHMMSS.dump
```
