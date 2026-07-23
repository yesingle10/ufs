package com.ufs.core.model.dto.policy;

import java.util.List;

/**
 * 更新存储策略请求（部分字段，对齐 OpenAPI Policy.Update）。
 */
public record UpdatePolicyRequest(
        String provider,
        String bucket,
        String pathTemplate,
        Long maxFileSize,
        List<String> allowedTypes,
        Integer lifecycleDays,
        String cdnDomain,
        Boolean active) {
}
