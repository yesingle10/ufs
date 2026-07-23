package com.ufs.core.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ufs.core.config.properties.UfsProperties;
import com.ufs.core.repository.entity.AuditLog;
import com.ufs.core.repository.mapper.AuditLogMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 生产审计发布器（@Profile("prod")）：先落库 audit_log（系统记录），再发到 RocketMQ 由审计服务索引 ES
 * （对齐 Java 规范 §9 / 安全规范 §7）。依赖 rocketmq-client（已在 pom 引入）。开发/预发使用 {@link LoggingAuditPublisher}。
 */
@Component
@Profile("prod")
public class RocketMqAuditPublisher implements AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(RocketMqAuditPublisher.class);

    private final UfsProperties props;
    private final ObjectMapper objectMapper;
    private final AuditLogMapper auditLogMapper;
    private DefaultMQProducer producer;

    public RocketMqAuditPublisher(UfsProperties props, ObjectMapper objectMapper, AuditLogMapper auditLogMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.auditLogMapper = auditLogMapper;
    }

    @PostConstruct
    public void init() throws MQClientException {
        UfsProperties.Rocketmq r = props.getRocketmq();
        producer = new DefaultMQProducer(r.getProducerGroup());
        producer.setNamesrvAddr(r.getNamesrvAddr());
        producer.start();
        log.info("RocketMqAuditPublisher started, namesrv={}", r.getNamesrvAddr());
    }

    @Override
    public void publish(AuditEvent event) {
        // 系统记录先落库
        auditLogMapper.insert(new AuditLog(null, event.tenantId(), event.operatorId(), event.operatorType(),
                event.action(), event.fileId(), event.objectKey(), event.clientIp(), event.requestId(),
                System.currentTimeMillis()));
        try {
            byte[] body = objectMapper.writeValueAsBytes(event);
            Message msg = new Message(props.getRocketmq().getAuditTopic(), body);
            producer.send(msg, 3000);
        } catch (Exception e) {
            log.error("audit mq publish failed, db record kept", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (producer != null) {
            producer.shutdown();
        }
    }
}
