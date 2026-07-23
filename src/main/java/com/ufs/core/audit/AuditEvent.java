package com.ufs.core.audit;

/**
 * 审计事件（对齐 audit_log 表字段 / 契约 §7.6）。经消息队列异步落 ES。
 */
public record AuditEvent(
        String tenantId,
        String operatorId,
        String operatorType,
        String action,
        String fileId,
        String objectKey,
        String clientIp,
        String requestId) {
}
