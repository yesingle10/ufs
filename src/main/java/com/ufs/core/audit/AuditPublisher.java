package com.ufs.core.audit;

/**
 * 审计事件发布器（异步落 ES，对齐 Java 规范 §9 / 安全规范 §7）。具体实现由部署环境决定。
 */
public interface AuditPublisher {

    void publish(AuditEvent event);
}
