-- UFS 元数据层 H2 兼容 DDL（dev-local profile 用，对齐 PRD §8 / SQL 规范 §3-§4）
-- H2 MODE=MySQL 下省略 ENGINE/CHARSET/COLLATE；其余结构与生产 MySQL DDL 一致。

CREATE TABLE IF NOT EXISTS file_meta (
    file_id      VARCHAR(64)  NOT NULL,
    tenant_id    VARCHAR(32)  NOT NULL,
    biz_type     VARCHAR(64)  DEFAULT '',
    provider     VARCHAR(16)  DEFAULT '',
    bucket       VARCHAR(128) DEFAULT '',
    object_key   VARCHAR(512) NOT NULL,
    file_name    VARCHAR(256) DEFAULT '',
    file_size    BIGINT       NOT NULL DEFAULT 0,
    content_type VARCHAR(128) DEFAULT '',
    md5          VARCHAR(64)  DEFAULT '',
    width        INT          NULL,
    height       INT          NULL,
    duration     INT          NULL,
    url          VARCHAR(1024) DEFAULT '',
    status       TINYINT      NOT NULL DEFAULT 0,
    custom_data  CLOB         NULL,
    callback_url VARCHAR(512) DEFAULT '',
    callback_status TINYINT   NOT NULL DEFAULT 0,
    created_at   BIGINT  NOT NULL DEFAULT 0,
    updated_at   BIGINT  NOT NULL DEFAULT 0,
    deleted_at   BIGINT  NULL,
    PRIMARY KEY (file_id)
);
CREATE INDEX IF NOT EXISTS idx_fm_tenant_biz_ct ON file_meta (tenant_id, biz_type, created_at);
CREATE INDEX IF NOT EXISTS idx_fm_object_key ON file_meta (object_key);

CREATE TABLE IF NOT EXISTS upload_task (
    upload_id      VARCHAR(64)  NOT NULL,
    file_id       VARCHAR(64)  DEFAULT '',
    tenant_id     VARCHAR(32)  NOT NULL,
    provider      VARCHAR(16)  DEFAULT '',
    bucket        VARCHAR(128) DEFAULT '',
    object_key    VARCHAR(512) NOT NULL,
    upload_mode   VARCHAR(16)  DEFAULT 'multipart',
    cloud_upload_id VARCHAR(256) DEFAULT '',
    total_size    BIGINT       NOT NULL DEFAULT 0,
    part_size     INT          NOT NULL DEFAULT 0,
    total_parts   INT          NOT NULL DEFAULT 0,
    completed_parts CLOB       NULL,
    status        TINYINT      NOT NULL DEFAULT 0,
    updated_at    BIGINT  NOT NULL DEFAULT 0,
    expires_at    BIGINT  NOT NULL DEFAULT 0,
    created_at    BIGINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (upload_id)
);
CREATE INDEX IF NOT EXISTS idx_ut_file_id ON upload_task (file_id);
CREATE INDEX IF NOT EXISTS idx_ut_expires_at ON upload_task (expires_at);

CREATE TABLE IF NOT EXISTS storage_policy (
    policy_id     VARCHAR(64)  NOT NULL,
    tenant_id     VARCHAR(32)  NOT NULL,
    biz_type      VARCHAR(64)  NOT NULL,
    provider      VARCHAR(16)  DEFAULT '',
    bucket        VARCHAR(128) DEFAULT '',
    path_template VARCHAR(256) DEFAULT '',
    max_file_size BIGINT       NOT NULL DEFAULT 0,
    allowed_types CLOB         NULL,
    lifecycle_days INT         NOT NULL DEFAULT 0,
    cdn_domain    VARCHAR(256) DEFAULT '',
    is_active     TINYINT      NOT NULL DEFAULT 1,
    created_at    BIGINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (policy_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sp_tenant_biz ON storage_policy (tenant_id, biz_type);

CREATE TABLE IF NOT EXISTS audit_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    VARCHAR(32)  NOT NULL,
    operator_id  VARCHAR(64)  DEFAULT '',
    operator_type VARCHAR(16) DEFAULT 'user',
    action       VARCHAR(32)  DEFAULT '',
    file_id      VARCHAR(64)  DEFAULT '',
    object_key   VARCHAR(512) DEFAULT '',
    client_ip    VARCHAR(64)  DEFAULT '',
    request_id   VARCHAR(64)  DEFAULT '',
    created_at   BIGINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_al_tenant_ct ON audit_log (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_al_operator_id ON audit_log (operator_id);
CREATE INDEX IF NOT EXISTS idx_al_action ON audit_log (action);
