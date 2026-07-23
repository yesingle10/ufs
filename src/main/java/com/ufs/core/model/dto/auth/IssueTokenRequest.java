package com.ufs.core.model.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 签发 JWT 请求：用 appKey + appSecret（HMAC 校验）换取短期 JWT（对齐契约 §3 双模式）。
 */
public record IssueTokenRequest(
        @NotBlank String appKey,
        @NotBlank String appSecret,
        String operatorId) {
}
