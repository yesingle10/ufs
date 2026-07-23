package com.ufs.core.scheduler;

import com.ufs.core.repository.entity.UploadTask;
import com.ufs.core.repository.mapper.UploadTaskMapper;
import com.ufs.core.tenant.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 过期上传事务清理（对齐 SQL 规范 §4：upload_task.expires_at 默认 24h）。
 * 以系统上下文执行，豁免租户守卫拦截器（跨租户扫描）。
 */
@Component
public class UploadTaskCleanup {

    private static final Logger log = LoggerFactory.getLogger(UploadTaskCleanup.class);

    private final UploadTaskMapper mapper;

    public UploadTaskCleanup(UploadTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void cleanup() {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        TenantContextHolder.set(TenantContextHolder.Context.system(requestId));
        try {
            long now = System.currentTimeMillis();
            List<UploadTask> expired = mapper.selectExpired(now);
            for (UploadTask t : expired) {
                mapper.delete(t.tenantId(), t.uploadId());
                log.info("cleaned expired upload_task {}", t.uploadId());
            }
        } finally {
            TenantContextHolder.clear();
            MDC.remove("requestId");
        }
    }
}
