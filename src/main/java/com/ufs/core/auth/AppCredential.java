package com.ufs.core.auth;

/**
 * 应用凭证：appKey 对应的租户与密钥（来自 KMS，绝不落库）。
 */
public record AppCredential(String tenantId, String appSecret) {
}
