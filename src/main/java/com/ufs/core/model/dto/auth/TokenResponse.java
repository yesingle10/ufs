package com.ufs.core.model.dto.auth;

/**
 * JWT 响应（前端持有，短期、只读/临时任务用）。
 */
public record TokenResponse(String token, long expiresAt, String tenantId) {
}
