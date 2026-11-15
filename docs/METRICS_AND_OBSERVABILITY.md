# CloudVault Observability & Metrics Guide

CloudVault exposes Spring Boot Actuator metrics consumable by Prometheus and Grafana.

---

## 1. Actuator Endpoints

- `/actuator/health`: High-level service health and storage backend stat probe.
- `/actuator/prometheus`: OpenMetrics/Prometheus scraper endpoint (restricted to ADMIN).
- `/actuator/metrics`: Ad-hoc query interface for JVM memory, threads, and HTTP latency.

---

## 2. Key Storage Metrics
- `storage_total_bytes`: Total configured block capacity across storage locations.
- `storage_used_bytes`: Current consumed storage bytes.
- `http_server_requests_seconds`: Request latency histogram broken down by endpoint and status code.
