package com.ufs.core.audit;

import com.ufs.core.repository.entity.AuditLog;
import com.ufs.core.repository.mapper.AuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 开发期审计发布器（默认激活）：落库 audit_log + 打日志，不依赖 RocketMQ。
 * 生产用 {@link RocketMqAuditPublisher}（@Profile("prod")）发送到消息队列异步落 ES。
 * DB 为审计系统记录（对齐安全规范 §7 / 契约 §7.6）。
 */
@Primary
@Component
public class LoggingAuditPublisher implements AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditPublisher.class);

    private final AuditLogMapper auditLogMapper;

    public LoggingAuditPublisher(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void publish(AuditEvent event) {
        auditLogMapper.insert(new AuditLog(null, event.tenantId(), event.operatorId(), event.operatorType(),
                event.action(), event.fileId(), event.objectKey(), event.clientIp(), event.requestId(),
                System.currentTimeMillis()));
        log.info("AUDIT tenant={} op={} action={} file={} key={} reqId={}",
                event.tenantId(), event.operatorId(), event.action(), event.fileId(), event.objectKey(), event.requestId());
    }
}
