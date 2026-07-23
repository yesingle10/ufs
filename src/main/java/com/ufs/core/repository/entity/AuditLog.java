package com.ufs.core.repository.entity;

import com.ufs.core.tenant.TenantScoped;

/**
 * audit_log 持久化实体（对齐 PRD §8.4 / SQL 规范）。关键操作审计，落 ES 前先落 MySQL 兜底。
 */
public record AuditLog(
        Long id,
        String tenantId,
        String operatorId,
        String operatorType,
        String action,
        String fileId,
        String objectKey,
        String clientIp,
        String requestId,
        Long createdAt) implements TenantScoped {

    @Override
    public String getTenantId() {
        return tenantId;
    }
}
