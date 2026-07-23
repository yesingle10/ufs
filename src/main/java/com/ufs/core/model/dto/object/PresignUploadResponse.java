package com.ufs.core.model.dto.object;

import java.util.Map;

/**
 * 预签名上传响应：简单上传返回单个 url；分片上传返回 uploadId + 各 part 的预签名 url。
 */
public record PresignUploadResponse(
        String fileId,
        String uploadId,
        String objectKey,
        String bucket,
        String method,
        String url,
        Map<Integer, String> partUrls,
        long expiresAt) {
}
