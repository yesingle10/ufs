package com.ufs.core.service.impl;

import com.ufs.core.common.PageResult;
import com.ufs.core.model.dto.audit.AuditLogDto;
import com.ufs.core.repository.entity.AuditLog;
import com.ufs.core.repository.mapper.AuditLogMapper;
import com.ufs.core.service.AuditService;
import com.ufs.core.tenant.TenantContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审计服务实现：游标分页检索（对齐 SQL 规范 §6 游标分页）。
 */
@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogMapper mapper;

    public AuditServiceImpl(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<AuditLogDto> list(String operatorId, String action, String cursor, int limit) {
        String tenant = TenantContextHolder.getTenantId();
        long c = cursor == null ? Long.MAX_VALUE : Long.parseLong(cursor);
        int n = limit <= 0 ? 20 : Math.min(limit, 200);
        List<AuditLog> rows = mapper.listByCursor(tenant, operatorId, action, c, n + 1);
        boolean hasMore = rows.size() > n;
        List<AuditLog> page = hasMore ? rows.subList(0, n) : rows;
        String next = hasMore ? String.valueOf(page.get(page.size() - 1).createdAt()) : null;
        return PageResult.of(page.stream().map(AuditLogDto::from).toList(), next, hasMore, n);
    }
}
