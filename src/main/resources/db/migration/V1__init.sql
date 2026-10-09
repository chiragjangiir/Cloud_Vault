-- Cloud Vault — core schema
-- All application state lives here; schema is managed exclusively by Flyway.

-- ---------------------------------------------------------------------------
-- Identity
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    username            VARCHAR(64)  NOT NULL,
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(100) NOT NULL,
    role                VARCHAR(16)  NOT NULL DEFAULT 'USER',
    status              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    display_name        VARCHAR(128),
    failed_login_count  INT          NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_username ON users (lower(username));
CREATE UNIQUE INDEX ux_users_email    ON users (lower(email));

CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,     -- sha256 of the raw token; raw token is never stored or logged
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_reset_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX ix_reset_tokens_user ON password_reset_tokens (user_id);

-- ---------------------------------------------------------------------------
-- Subscriptions / plans (data-driven, admin-editable, enforced server-side)
-- ---------------------------------------------------------------------------
CREATE TABLE plans (
    id                     BIGSERIAL PRIMARY KEY,
    code                   VARCHAR(32)  NOT NULL UNIQUE,
    name                   VARCHAR(64)  NOT NULL,
    storage_bytes          BIGINT       NOT NULL CHECK (storage_bytes >= 0),
    max_file_bytes         BIGINT       NOT NULL CHECK (max_file_bytes > 0),
    sharing_enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    versioning_enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    max_versions           INT          NOT NULL DEFAULT 1 CHECK (max_versions >= 1),
    api_access             BOOLEAN      NOT NULL DEFAULT TRUE,
    retention_days         INT          NOT NULL DEFAULT 7,
    max_downloads_per_day  INT,
    sort_order             INT          NOT NULL DEFAULT 0
);

CREATE TABLE subscriptions (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    plan_id              BIGINT      NOT NULL REFERENCES plans(id),
    status               VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    storage_bytes_override BIGINT,                  -- admin quota override; NULL = use plan value
    started_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    current_period_start TIMESTAMPTZ NOT NULL DEFAULT now(),
    current_period_end   TIMESTAMPTZ,
    trial_ends_at        TIMESTAMPTZ,
    cancelled_at         TIMESTAMPTZ,
    admin_note           VARCHAR(500),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_subscriptions_status ON subscriptions (status);

-- ---------------------------------------------------------------------------
-- Storage infrastructure
-- ---------------------------------------------------------------------------
CREATE TABLE storage_locations (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(64)  NOT NULL UNIQUE,
    root_path           VARCHAR(1024) NOT NULL,
    provider            VARCHAR(32)  NOT NULL DEFAULT 'local-filesystem',
    status              VARCHAR(16)  NOT NULL DEFAULT 'OFFLINE',
    total_capacity_bytes BIGINT      NOT NULL DEFAULT 0,
    usable_capacity_bytes BIGINT     NOT NULL DEFAULT 0,
    filesystem          VARCHAR(64),
    access_mode         VARCHAR(8)   NOT NULL DEFAULT 'RW',   -- RW | RO
    is_writable         BOOLEAN      NOT NULL DEFAULT FALSE,
    last_health_check   TIMESTAMPTZ,
    last_health_message VARCHAR(500),
    auto_registered     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE storage_pools (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255),
    status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE storage_pool_locations (
    pool_id        BIGINT NOT NULL REFERENCES storage_pools(id) ON DELETE CASCADE,
    location_id    BIGINT NOT NULL REFERENCES storage_locations(id) ON DELETE RESTRICT,
    priority       INT    NOT NULL DEFAULT 100,
    PRIMARY KEY (pool_id, location_id)
);

CREATE TABLE storage_objects (
    id                 BIGSERIAL PRIMARY KEY,
    pool_id            BIGINT       NOT NULL REFERENCES storage_pools(id),
    location_id        BIGINT       NOT NULL REFERENCES storage_locations(id),
    storage_key        VARCHAR(255) NOT NULL UNIQUE,   -- generated by Cloud Vault, never user input
    physical_reference VARCHAR(1024) NOT NULL,          -- absolute path resolved at write time
    size_bytes         BIGINT       NOT NULL CHECK (size_bytes >= 0),
    checksum_sha256    VARCHAR(64)  NOT NULL,
    content_type       VARCHAR(128),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    verified_at        TIMESTAMPTZ
);
CREATE INDEX ix_objects_location ON storage_objects (location_id);

-- ---------------------------------------------------------------------------
-- User quota accounting (hot path counters; reconciled against
-- storage_objects by the background reconciliation job)
-- ---------------------------------------------------------------------------
CREATE TABLE user_usage (
    user_id    BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    used_bytes BIGINT      NOT NULL DEFAULT 0 CHECK (used_bytes >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE quota_reservations (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bytes       BIGINT      NOT NULL CHECK (bytes > 0),
    status      VARCHAR(16) NOT NULL DEFAULT 'RESERVED',  -- RESERVED | COMMITTED | RELEASED
    reason      VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ
);
CREATE INDEX ix_reservations_user_status ON quota_reservations (user_id, status);

-- ---------------------------------------------------------------------------
-- Files and folders (database-backed hierarchy, soft delete / trash)
-- ---------------------------------------------------------------------------
CREATE TABLE folders (
    id           BIGSERIAL PRIMARY KEY,
    owner_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id    BIGINT       REFERENCES folders(id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,
    retention_until TIMESTAMPTZ,
    original_parent_id BIGINT REFERENCES folders(id) ON DELETE SET NULL,
    CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE INDEX ix_folders_owner_parent ON folders (owner_id, parent_id);
CREATE UNIQUE INDEX ux_folders_live_name ON folders (owner_id, coalesce(parent_id, 0), lower(name)) WHERE deleted_at IS NULL;

CREATE TABLE files (
    id                BIGSERIAL PRIMARY KEY,
    owner_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    folder_id         BIGINT       NOT NULL REFERENCES folders(id),
    name              VARCHAR(255) NOT NULL,
    content_type      VARCHAR(128) NOT NULL DEFAULT 'application/octet-stream',
    active_version_id BIGINT,           -- FK added after file_versions
    size_bytes        BIGINT       NOT NULL DEFAULT 0,
    checksum_sha256   VARCHAR(64),
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | TRASHED
    version_count     INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    retention_until   TIMESTAMPTZ,
    original_folder_id BIGINT REFERENCES folders(id) ON DELETE SET NULL
);
CREATE INDEX ix_files_owner_folder ON files (owner_id, folder_id);
CREATE INDEX ix_files_status ON files (status);
CREATE INDEX ix_files_name_search ON files (lower(name));
CREATE UNIQUE INDEX ux_files_live_name ON files (folder_id, lower(name)) WHERE status = 'ACTIVE';

CREATE TABLE file_versions (
    id                BIGSERIAL PRIMARY KEY,
    file_id           BIGINT      NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    version_number    INT         NOT NULL,
    size_bytes        BIGINT      NOT NULL,
    checksum_sha256   VARCHAR(64) NOT NULL,
    content_type      VARCHAR(128) NOT NULL,
    storage_object_id BIGINT      NOT NULL REFERENCES storage_objects(id),
    created_by        BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (file_id, version_number)
);
ALTER TABLE files ADD CONSTRAINT fk_files_active_version
    FOREIGN KEY (active_version_id) REFERENCES file_versions(id) ON DELETE SET NULL;

-- ---------------------------------------------------------------------------
-- Sharing
-- ---------------------------------------------------------------------------
CREATE TABLE shares (
    id                BIGSERIAL PRIMARY KEY,
    file_id           BIGINT       NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    owner_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type              VARCHAR(16)  NOT NULL,               -- PUBLIC_LINK | USER_SHARE
    permission        VARCHAR(16)  NOT NULL,               -- VIEW | DOWNLOAD | EDIT
    token             VARCHAR(64),                          -- cryptographically random, public links only
    recipient_id      BIGINT       REFERENCES users(id) ON DELETE CASCADE,
    expires_at        TIMESTAMPTZ,
    revoked_at        TIMESTAMPTZ,
    max_downloads     INT,
    download_count    INT          NOT NULL DEFAULT 0,
    created_by        BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (token IS NOT NULL OR recipient_id IS NOT NULL),
    CHECK (type <> 'PUBLIC_LINK' OR token IS NOT NULL),
    CHECK (type <> 'USER_SHARE'  OR recipient_id IS NOT NULL)
);
CREATE UNIQUE INDEX ux_share_token ON shares (token) WHERE token IS NOT NULL;
CREATE INDEX ix_shares_owner ON shares (owner_id);
CREATE INDEX ix_shares_recipient ON shares (recipient_id);

-- ---------------------------------------------------------------------------
-- Background jobs
-- ---------------------------------------------------------------------------
CREATE TABLE background_jobs (
    id               BIGSERIAL PRIMARY KEY,
    type             VARCHAR(48)  NOT NULL,
    state            VARCHAR(16)  NOT NULL DEFAULT 'QUEUED',  -- QUEUED | RUNNING | COMPLETED | FAILED | CANCELLED
    payload          TEXT,
    progress_processed BIGINT     NOT NULL DEFAULT 0,
    progress_total   BIGINT       NOT NULL DEFAULT 0,
    bytes_processed  BIGINT       NOT NULL DEFAULT 0,
    bytes_total      BIGINT       NOT NULL DEFAULT 0,
    error_message    VARCHAR(2000),
    created_by       BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ
);
CREATE INDEX ix_jobs_state ON background_jobs (state, created_at);
CREATE INDEX ix_jobs_type ON background_jobs (type, created_at);

-- ---------------------------------------------------------------------------
-- Notifications and audit
-- ---------------------------------------------------------------------------
CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(48)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       VARCHAR(1000),
    link       VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    read_at    TIMESTAMPTZ
);
CREATE INDEX ix_notifications_user ON notifications (user_id, created_at DESC);

CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    actor_id     BIGINT,
    actor_username VARCHAR(64),
    action       VARCHAR(64)  NOT NULL,
    target_type  VARCHAR(64),
    target_id    VARCHAR(64),
    details      VARCHAR(2000),
    ip_address   VARCHAR(64),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_created ON audit_logs (created_at DESC);
CREATE INDEX ix_audit_actor ON audit_logs (actor_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- Rate limiting (fixed-window counters)
-- ---------------------------------------------------------------------------
CREATE TABLE rate_limit_counters (
    bucket_key   VARCHAR(255) NOT NULL,
    window_start TIMESTAMPTZ  NOT NULL,
    hits         INT          NOT NULL DEFAULT 0,
    last_seen_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (bucket_key, window_start)
);

-- ---------------------------------------------------------------------------
-- Session registry (externalized to DB so restarts do not log everyone out
-- and so admins can invalidate sessions)
-- ---------------------------------------------------------------------------
CREATE TABLE user_sessions (
    id           VARCHAR(100) PRIMARY KEY,
    user_id      BIGINT      REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address   VARCHAR(64),
    user_agent   VARCHAR(300)
);
CREATE INDEX ix_user_sessions_user ON user_sessions (user_id);
