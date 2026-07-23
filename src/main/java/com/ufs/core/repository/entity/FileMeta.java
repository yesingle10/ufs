package com.ufs.core.repository.entity;

import com.ufs.core.tenant.TenantScoped;

/**
 * file_meta 持久化实体（对齐 PRD §8.1 / SQL 规范）。实现 {@link TenantScoped} 供租户守卫拦截器校验。
 * 注意：DTO 用 record（Java 规范 §7），Entity 仅用于持久化，禁止直接序列化出 API。
 */
public record FileMeta(
        String fileId,
        String tenantId,
        String bizType,
        String provider,
        String bucket,
        String objectKey,
        String fileName,
        long fileSize,
        String contentType,
        String md5,
        Integer width,
        Integer height,
        Integer duration,
        String url,
        int status,
        String customData,
        String callbackUrl,
        int callbackStatus,
        Long createdAt,
        Long updatedAt,
        Long deletedAt) implements TenantScoped {

    @Override
    public String getTenantId() {
        return tenantId;
    }
}
