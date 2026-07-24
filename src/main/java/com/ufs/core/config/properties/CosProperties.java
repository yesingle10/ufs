package com.ufs.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云 COS 配置属性（对齐 Java 规范 §10：密钥走配置中心/KMS，绝不落库）。
 */
@Component
@ConfigurationProperties(prefix = "ufs.cos")
public class CosProperties {

    /** 腾讯云 SecretId */
    private String secretId;

    /** 腾讯云 SecretKey */
    private String secretKey;

    /** COS 地域，如 ap-guangzhou */
    private String region = "ap-guangzhou";

    /** 默认存储桶名称 */
    private String defaultBucket;

    /** 预签名 URL 有效期（秒），默认 1 小时 */
    private long presignExpireSeconds = 3600;

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public void setDefaultBucket(String defaultBucket) {
        this.defaultBucket = defaultBucket;
    }

    public long getPresignExpireSeconds() {
        return presignExpireSeconds;
    }

    public void setPresignExpireSeconds(long presignExpireSeconds) {
        this.presignExpireSeconds = presignExpireSeconds;
    }
}
