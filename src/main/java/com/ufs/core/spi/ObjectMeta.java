package com.ufs.core.spi;

/**
 * 对象元数据领域对象（跨 spi/service 使用，不直接暴露厂商私有类型，对齐 Java 规范 §6）。
 * 字段对齐 file_meta 表与 OpenAPI ObjectMeta。
 */
public record ObjectMeta(
        String fileId,
        String tenantId,
        String bucket,
        String objectKey,
        String fileName,
        long fileSize,
        String contentType,
        String md5,
        Integer width,
        Integer height,
        Integer duration,
        String url,
        int status,
        long createdAt,
        long updatedAt) {
}
