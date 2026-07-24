package com.ufs.core.service;

import com.ufs.core.common.PageResult;
import com.ufs.core.model.dto.audit.AuditLogDto;
import com.ufs.core.repository.entity.AuditLog;
import com.ufs.core.repository.mapper.AuditLogMapper;
import com.ufs.core.service.impl.AuditServiceImpl;
import com.ufs.core.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("AuditService 单元测试")
@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock private AuditLogMapper mapper;

    private AuditServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.set(new TenantContextHolder.Context("t_demo", "op", "req-01", "app_demo", false));
        service = new AuditServiceImpl(mapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("list: 返回审计日志分页结果")
    void shouldReturnAuditPage() {
        long now = Instant.now().toEpochMilli();
        AuditLog log1 = new AuditLog(1L, "t_demo", "admin", "user", "bucket.create",
                "f_001", "test/file.txt", "127.0.0.1", "req-001", now);
        AuditLog log2 = new AuditLog(2L, "t_demo", "admin", "user", "bucket.delete",
                "f_002", "test/old.txt", "127.0.0.2", "req-002", now - 1000);

        when(mapper.listByCursor("t_demo", null, null, Long.MAX_VALUE, 6))
                .thenReturn(List.of(log1, log2));

        PageResult<AuditLogDto> page = service.list(null, null, null, 5);

        assertThat(page.getList()).hasSize(2);
        assertThat(page.isHasMore()).isFalse();
        assertThat(page.getList().get(0).action()).isEqualTo("bucket.create");
        assertThat(page.getList().get(1).objectKey()).isEqualTo("test/old.txt");
    }

    @Test
    @DisplayName("list: hasMore=true 且限制返回数量")
    void shouldReturnHasMoreWhenMoreResults() {
        long now = Instant.now().toEpochMilli();
        AuditLog log = new AuditLog(1L, "t_demo", "op", "user", "policy.create",
                null, null, "10.0.0.1", "req-01", now);
        // mapper 返回 n+1=3 条，limit=2，说明有更多
        when(mapper.listByCursor("t_demo", "admin", "policy.create", 100L, 3))
                .thenReturn(List.of(log, log, log));

        PageResult<AuditLogDto> page = service.list("admin", "policy.create", "100", 2);

        assertThat(page.getList()).hasSize(2); // 截断为 2
        assertThat(page.isHasMore()).isTrue();
    }
}
