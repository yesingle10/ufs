-- UFS 元数据层初始化 DDL（对齐 PRD §8 / SQL 规范 §3-§4）
-- 引擎 InnoDB、utf8mb4、DATETIME(3) 毫秒精度；业务主键 VARCHAR(32)；tenant_id 分片键；状态列带 COMMENT。
-- 分库分表在基础设施层（ShardingSphere，按 tenant_id 取模 16 库、file_meta 月度分表），此处为单 schema 表结构。

CREATE TABLE IF NOT EXISTS file_meta (
    file_id      VARCHAR(32)  NOT NULL,
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
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=上传中 1=成功 2=失败 3=已删除',
    custom_data  JSON         NULL,
    callback_url VARCHAR(512) DEFAULT '',
    callback_status TINYINT   NOT NULL DEFAULT 0 COMMENT '0=未回调 1=成功 2=重试中 3=失败',
    created_at   DATETIME(3)  NOT NULL,
    updated_at   DATETIME(3)  NOT NULL,
    deleted_at   DATETIME(3)  NULL,
    PRIMARY KEY (file_id),
    KEY idx_tenant_biz_ct (tenant_id, biz_type, created_at),
    KEY idx_object_key (object_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件元数据';

CREATE TABLE IF NOT EXISTS upload_task (
    upload_id      VARCHAR(32)  NOT NULL,
    file_id       VARCHAR(32)  DEFAULT '',
    tenant_id     VARCHAR(32)  NOT NULL,
    provider      VARCHAR(16)  DEFAULT '',
    bucket        VARCHAR(128) DEFAULT '',
    object_key    VARCHAR(512) NOT NULL,
    upload_mode   VARCHAR(16)  DEFAULT 'multipart',
    cloud_upload_id VARCHAR(256) DEFAULT '',
    total_size    BIGINT       NOT NULL DEFAULT 0,
    part_size     INT          NOT NULL DEFAULT 0,
    total_parts   INT          NOT NULL DEFAULT 0,
    completed_parts JSON        NULL,
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=进行中 1=已完成 2=已取消 3=已过期',
    expires_at    DATETIME(3)  NOT NULL,
    created_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (upload_id),
    KEY idx_file_id (file_id),
    KEY idx_expires_at (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '上传事务';

CREATE TABLE IF NOT EXISTS storage_policy (
    policy_id     VARCHAR(32)  NOT NULL,
    tenant_id     VARCHAR(32)  NOT NULL,
    biz_type      VARCHAR(64)  NOT NULL,
    provider      VARCHAR(16)  DEFAULT '',
    bucket        VARCHAR(128) DEFAULT '',
    path_template VARCHAR(256) DEFAULT '',
    max_file_size BIGINT       NOT NULL DEFAULT 0,
    allowed_types JSON         NULL,
    lifecycle_days INT         NOT NULL DEFAULT 0 COMMENT '0=永久',
    cdn_domain    VARCHAR(256) DEFAULT '',
    is_active     TINYINT      NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
    created_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (policy_id),
    UNIQUE KEY uk_tenant_biz (tenant_id, biz_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存储策略';

CREATE TABLE IF NOT EXISTS audit_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    VARCHAR(32)  NOT NULL,
    operator_id  VARCHAR(64)  DEFAULT '',
    operator_type VARCHAR(16) DEFAULT 'user' COMMENT 'user/service',
    action       VARCHAR(32)  DEFAULT '' COMMENT 'upload/download/delete/copy/move',
    file_id      VARCHAR(32)  DEFAULT '',
    object_key   VARCHAR(512) DEFAULT '',
    client_ip    VARCHAR(64)  DEFAULT '',
    request_id   VARCHAR(64)  DEFAULT '',
    created_at   DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tenant_ct (tenant_id, created_at),
    KEY idx_operator_id (operator_id),
    KEY idx_action (action)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审计日志';
