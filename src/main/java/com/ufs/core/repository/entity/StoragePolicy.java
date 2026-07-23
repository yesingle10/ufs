package com.ufs.core.repository.entity;

import com.ufs.core.tenant.TenantScoped;

/**
 * storage_policy 持久化实体（对齐 PRD §8.3 / SQL 规范）。按 bizType 路由到厂商/Bucket。
 */
public record StoragePolicy(
        String policyId,
        String tenantId,
        String bizType,
        String provider,
        String bucket,
        String pathTemplate,
        long maxFileSize,
        String allowedTypes,
        int lifecycleDays,
        String cdnDomain,
        int isActive,
        Long createdAt) implements TenantScoped {

    @Override
    public String getTenantId() {
        return tenantId;
    }
}
