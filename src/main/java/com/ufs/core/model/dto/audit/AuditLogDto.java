package com.ufs.core.model.dto.audit;

import com.ufs.core.repository.entity.AuditLog;

/**
 * 审计日志响应 DTO。
 */
public record AuditLogDto(
        Long id,
        String tenantId,
        String operatorId,
        String operatorType,
        String action,
        String fileId,
        String objectKey,
        String clientIp,
        String requestId,
        long createdAt) {

    public static AuditLogDto from(AuditLog a) {
        return new AuditLogDto(a.id(), a.tenantId(), a.operatorId(), a.operatorType(), a.action(),
                a.fileId(), a.objectKey(), a.clientIp(), a.requestId(), a.createdAt());
    }
}
