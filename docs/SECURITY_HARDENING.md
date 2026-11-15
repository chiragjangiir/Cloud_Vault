# CloudVault Security Hardening Guide

Operational guidelines for running CloudVault securely in untrusted network environments.

---

## 1. TLS and Reverse Proxy Hardening
- Terminate TLS 1.3 exclusively at the load balancer / reverse proxy.
- Set `Strict-Transport-Security: max-age=63072000; includeSubDomains; preload`.
- Ensure `X-Frame-Options: DENY` and `X-Content-Type-Options: nosniff` are passed.

---

## 2. Secrets Management
- Avoid committing database passwords or admin secrets in plain text.
- Use Docker secrets, Kubernetes secrets, or AWS Secrets Manager injected at container runtime.
