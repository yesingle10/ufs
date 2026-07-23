package com.ufs.core.model.dto.policy;

import java.util.List;

/**
 * 存储策略响应 DTO（字段对齐 PRD §8.3 / 控制台原型 POLICIES）。
 */
public record PolicyDto(
        String policyId,
        String tenantId,
        String bizType,
        String provider,
        String bucket,
        String pathTemplate,
        long maxFileSize,
        List<String> allowedTypes,
        int lifecycleDays,
        String cdnDomain,
        boolean active) {

    public static PolicyDto from(com.ufs.core.repository.entity.StoragePolicy p, java.util.List<String> allowed) {
        return new PolicyDto(p.policyId(), p.tenantId(), p.bizType(), p.provider(), p.bucket(),
                p.pathTemplate(), p.maxFileSize(), allowed, p.lifecycleDays(), p.cdnDomain(), p.isActive() == 1);
    }
}
