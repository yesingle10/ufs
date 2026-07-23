package com.ufs.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * UFS 配置属性（外部化，对齐 Java 规范 §10：密钥走配置中心/KMS，绝不入库）。
 */
@Component
@ConfigurationProperties(prefix = "ufs")
public class UfsProperties {

    /** JWT 签名密钥（HS256，需 ≥ 32 字节）。生产来自 KMS。 */
    private String jwtSecret = "dev-only-change-me-please-32bytes-min!!";

    /** JWT 有效期（秒），默认 2h（对齐安全规范 §1）。 */
    private long jwtTtlSeconds = 7200;

    /** HMAC 签名时间窗口 ±秒（对齐契约 §3）。 */
    private long signTtlSeconds = 300;

    /** nonce 去重窗口（秒），防重放。 */
    private long nonceTtlSeconds = 300;

    /** 开发期凭证（生产由 KMS 注入，绝不落库）。 */
    private Map<String, AppCredential> appCredentials = new HashMap<>();

    private final Rocketmq rocketmq = new Rocketmq();

    public record AppCredential(String tenantId, String appSecret) {
    }

    public static class Rocketmq {
        private String namesrvAddr = "localhost:9876";
        private String auditTopic = "ufs_audit";
        private String producerGroup = "ufs_core_producer";

        public String getNamesrvAddr() {
            return namesrvAddr;
        }

        public void setNamesrvAddr(String namesrvAddr) {
            this.namesrvAddr = namesrvAddr;
        }

        public String getAuditTopic() {
            return auditTopic;
        }

        public void setAuditTopic(String auditTopic) {
            this.auditTopic = auditTopic;
        }

        public String getProducerGroup() {
            return producerGroup;
        }

        public void setProducerGroup(String producerGroup) {
            this.producerGroup = producerGroup;
        }
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getJwtTtlSeconds() {
        return jwtTtlSeconds;
    }

    public void setJwtTtlSeconds(long jwtTtlSeconds) {
        this.jwtTtlSeconds = jwtTtlSeconds;
    }

    public long getSignTtlSeconds() {
        return signTtlSeconds;
    }

    public void setSignTtlSeconds(long signTtlSeconds) {
        this.signTtlSeconds = signTtlSeconds;
    }

    public long getNonceTtlSeconds() {
        return nonceTtlSeconds;
    }

    public void setNonceTtlSeconds(long nonceTtlSeconds) {
        this.nonceTtlSeconds = nonceTtlSeconds;
    }

    public Map<String, AppCredential> getAppCredentials() {
        return appCredentials;
    }

    public void setAppCredentials(Map<String, AppCredential> appCredentials) {
        this.appCredentials = appCredentials;
    }

    public Rocketmq getRocketmq() {
        return rocketmq;
    }
}
