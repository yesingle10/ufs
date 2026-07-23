package com.ufs.core.model.dto.bucket;

import java.util.List;

/**
 * 存储桶响应 DTO（字段对齐控制台原型 BUCKETS 数据模型）。
 */
public record BucketDto(
        String name,
        String vendor,
        String region,
        String cdnDomain,
        String encryption,
        boolean versioning,
        boolean crr,
        int lifecycleDays,
        String acl,
        List<String> cors,
        String note,
        long objectCount,
        long storageBytes) {
}
