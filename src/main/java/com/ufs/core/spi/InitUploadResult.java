package com.ufs.core.spi;

/**
 * 分片上传初始化结果。
 */
public record InitUploadResult(String uploadId, int partSize, int totalParts) {
}
