# CloudVault HTTP Caching Architecture

- Static assets (`/css/**`, `/js/**`): `Cache-Control: public, max-age=31536000, immutable`
- Dynamic file downloads: Strong ETags computed from SHA-256 object digests.
