package com.ufs.core.auth;

/**
 * 凭证解析：appKey → 租户与密钥。生产由 KMS 实现，密钥不落库（对齐安全规范 §2）。
 */
public interface CredentialResolver {

    /**
     * @throws com.ufs.core.common.error.UfsException UNAUTHORIZED 凭证不存在/已吊销
     */
    AppCredential resolve(String appKey);
}
