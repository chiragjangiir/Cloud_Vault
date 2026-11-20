# Zero-Trust Storage Architecture Specification

## 1. Principles
Every data plane operation against CloudVault object stores requires:
- Cryptographic request signing (AWS SigV4).
- Envelope encryption with Customer Managed Keys (KMS).
- Explicit per-object attribute-based access control (ABAC).
