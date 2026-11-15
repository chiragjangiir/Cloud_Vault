# Kubernetes Liveness and Readiness Probe Configuration

- **Liveness**: `httpGet: { path: /actuator/health/liveness, port: 8080 }`
- **Readiness**: `httpGet: { path: /actuator/health/readiness, port: 8080 }`
