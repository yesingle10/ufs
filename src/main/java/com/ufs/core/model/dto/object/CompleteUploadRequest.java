package com.ufs.core.model.dto.object;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 完成上传请求（对齐 OpenAPI Object.CompleteUpload）。分片带 uploadId + 各 part ETag，简单上传不带。
 */
public record CompleteUploadRequest(
        String uploadId,
        @NotBlank String fileId,
        @NotBlank String objectKey,
        String bucket,
        @NotBlank String bizType,
        long fileSize,
        String md5,
        List<PartETagDto> parts) {

    /**
     * 分片 ETag（partNumber + etag），对齐 SPI CompleteContext.PartETag。
     */
    public record PartETagDto(int partNumber, String etag) {
    }
}
