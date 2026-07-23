package com.ufs.core.model.dto.bucket;

import java.util.List;

/**
 * 更新存储桶请求（部分字段，对齐 OpenAPI Bucket.Update）。
 */
public record UpdateBucketRequest(
        String cdnDomain,
        String encryption,
        Boolean versioning,
        Boolean crr,
        Integer lifecycleDays,
        String acl,
        List<String> cors,
        String note) {
}
