package com.ufs.core.model.dto.policy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 创建存储策略请求（对齐 OpenAPI Policy.Create）。bizType 为路由键。
 */
public record CreatePolicyRequest(
        @NotBlank String bizType,
        @Pattern(regexp = "oss|kodo|tos|local") String provider,
        @NotBlank String bucket,
        String pathTemplate,
        @Positive long maxFileSize,
        List<String> allowedTypes,
        Integer lifecycleDays,
        String cdnDomain) {
}
