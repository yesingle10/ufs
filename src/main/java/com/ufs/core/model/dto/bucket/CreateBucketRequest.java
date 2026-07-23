package com.ufs.core.model.dto.bucket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * 创建存储桶请求（对齐 OpenAPI Bucket.Create）。
 */
public record CreateBucketRequest(
        @NotBlank String name,
        @Pattern(regexp = "oss|kodo|tos|local") String vendor,
        String region,
        String cdnDomain,
        String encryption,
        Boolean versioning,
        Boolean crr,
        Integer lifecycleDays,
        @Pattern(regexp = "private|public-read|public-read-write") String acl,
        List<String> cors,
        String note) {
}
