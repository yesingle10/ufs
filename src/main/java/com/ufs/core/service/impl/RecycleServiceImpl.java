package com.ufs.core.service.impl;

import com.ufs.core.audit.AuditEvent;
import com.ufs.core.audit.AuditPublisher;
import com.ufs.core.common.PageResult;
import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.model.dto.object.ObjectDto;
import com.ufs.core.repository.entity.FileMeta;
import com.ufs.core.repository.mapper.FileMetaMapper;
import com.ufs.core.service.RecycleService;
import com.ufs.core.tenant.TenantContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 回收站服务实现：恢复=清除 deleted_at；彻底删除=物理删除（对齐 Java 规范 §5 统一异常）。
 */
@Service
public class RecycleServiceImpl implements RecycleService {

    private final FileMetaMapper mapper;
    private final AuditPublisher audit;

    public RecycleServiceImpl(FileMetaMapper mapper, AuditPublisher audit) {
        this.mapper = mapper;
        this.audit = audit;
    }

    @Override
    public PageResult<ObjectDto> list(int limit) {
        String tenant = TenantContextHolder.getTenantId();
        int n = limit <= 0 ? 20 : Math.min(limit, 200);
        List<FileMeta> rows = mapper.listDeleted(tenant, n);
        return PageResult.of(rows.stream().map(ObjectDto::from).toList(), (long) rows.size(), n);
    }

    @Override
    public void restore(String fileId) {
        String tenant = TenantContextHolder.getTenantId();
        if (mapper.selectById(tenant, fileId) == null) {
            throw new UfsException(UfsErrorCode.NOT_FOUND, "object not found");
        }
        mapper.restore(tenant, fileId, System.currentTimeMillis());
        audit.publish(new AuditEvent(tenant, TenantContextHolder.getOperatorId(), "service", "recycle.restore",
                fileId, "", "", TenantContextHolder.getRequestId()));
    }

    @Override
    public void purge(String fileId) {
        String tenant = TenantContextHolder.getTenantId();
        if (mapper.selectById(tenant, fileId) == null) {
            throw new UfsException(UfsErrorCode.NOT_FOUND, "object not found");
        }
        mapper.purge(tenant, fileId);
        audit.publish(new AuditEvent(tenant, TenantContextHolder.getOperatorId(), "service", "recycle.purge",
                fileId, "", "", TenantContextHolder.getRequestId()));
    }
}
