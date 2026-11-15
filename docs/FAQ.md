# CloudVault Frequently Asked Questions & Troubleshooting

---

### Q: Why is `/actuator/health` reporting status DOWN?
**A:** The `StorageHealthIndicator` performs live filesystem reads and writes to all enabled storage paths. If any path is read-only, missing, or full, health status drops to `DOWN`. Check container volume mount permissions.

---

### Q: How do I increase the maximum file upload size?
**A:** Adjust `MAX_FILE_SIZE=20GB` in your `.env` file and verify reverse proxy `client_max_body_size` matches.
