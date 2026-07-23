package com.ufs.core.model.dto.object;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 预签名上传请求（对齐 OpenAPI Object.PresignUpload）。
 */
public record PresignUploadRequest(
        String bucket,
        String objectKey,
        @NotBlank String fileName,
        String contentType,
        @Positive long fileSize,
        @NotBlank String bizType,
        boolean multipart,
        Integer partSize) {
}
