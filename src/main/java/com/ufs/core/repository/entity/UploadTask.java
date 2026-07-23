package com.ufs.core.repository.entity;

import com.ufs.core.tenant.TenantScoped;

/**
 * upload_task 持久化实体（对齐 PRD §8.2 / SQL 规范）。分片上传事务，过期由定时任务清理。
 */
public record UploadTask(
        String uploadId,
        String fileId,
        String tenantId,
        String provider,
        String bucket,
        String objectKey,
        String uploadMode,
        String cloudUploadId,
        long totalSize,
        int partSize,
        int totalParts,
        String completedParts,
        int status,
        Long expiresAt,
        Long createdAt) implements TenantScoped {

    @Override
    public String getTenantId() {
        return tenantId;
    }
}
