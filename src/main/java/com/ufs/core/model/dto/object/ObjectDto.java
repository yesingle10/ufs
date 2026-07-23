package com.ufs.core.model.dto.object;

import com.ufs.core.repository.entity.FileMeta;

/**
 * 对象响应 DTO（API 层不直接暴露 Entity，对齐 Java 规范 §7）。
 */
public record ObjectDto(
        String fileId,
        String tenantId,
        String bizType,
        String provider,
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

    public static ObjectDto from(FileMeta m) {
        return new ObjectDto(m.fileId(), m.tenantId(), m.bizType(), m.provider(), m.bucket(),
                m.objectKey(), m.fileName(), m.fileSize(), m.contentType(), m.md5(),
                m.width(), m.height(), m.duration(), m.url(), m.status(), m.createdAt(), m.updatedAt());
    }
}
