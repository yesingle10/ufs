package com.ufs.core.service;

import com.ufs.core.common.PageResult;
import com.ufs.core.model.dto.audit.AuditLogDto;

/**
 * 审计服务：检索审计日志（对齐契约 §7.6 / 安全规范 §7）。
 */
public interface AuditService {

    PageResult<AuditLogDto> list(String operatorId, String action, String cursor, int limit);
}
