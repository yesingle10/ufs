package com.ufs.core.spi;

import java.util.Map;

/**
 * 预签名结果：直传/直链 URL（UFS 不经手带宽，对齐契约“预签名直传优先”原则）。
 */
public record PresignResult(
        String url,
        String method,
        Map<String, String> headers,
        long expiresAt,
        String uploadId,
        String objectKey,
        String bucket) {
}
