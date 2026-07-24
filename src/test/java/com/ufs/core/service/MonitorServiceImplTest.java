package com.ufs.core.service;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.model.dto.monitor.MonitorResponse;
import com.ufs.core.repository.mapper.FileMetaMapper;
import com.ufs.core.service.impl.MonitorServiceImpl;
import com.ufs.core.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("MonitorService 单元测试")
@ExtendWith(MockitoExtension.class)
class MonitorServiceImplTest {

    @Mock private FileMetaMapper mapper;

    private MonitorServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.set(new TenantContextHolder.Context("t_demo", "op", "req-01", "app_demo", false));
        service = new MonitorServiceImpl(mapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("get: 返回完整的三图监控数据")
    void shouldReturnFullMonitorData() {
        when(mapper.sumBytes("t_demo")).thenReturn(10_737_418_240L); // 10 GB

        MonitorResponse resp = service.get();

        // 延迟数据: 30 个点
        assertThat(resp.latency()).hasSize(30);
        resp.latency().forEach(p -> {
            assertThat(p.ts()).isGreaterThan(0);
            assertThat(p.p95()).isGreaterThanOrEqualTo(p.p50());
            assertThat(p.p99()).isGreaterThanOrEqualTo(p.p95());
        });

        // 请求数据: 30 个点
        assertThat(resp.requests()).hasSize(30);
        resp.requests().forEach(p -> {
            assertThat(p.qps()).isGreaterThanOrEqualTo(0);
            assertThat(p.errorRatio()).isBetween(0.0, 1.0);
        });

        // 存储用量: 12 个月
        assertThat(resp.usage()).hasSize(12);
        long firstTotal = resp.usage().get(0).totalBytes();
        assertThat(firstTotal).isGreaterThan(0);
    }
}
