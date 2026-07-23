package com.ufs.core.service.impl;

import com.ufs.core.model.dto.monitor.MonitorResponse;
import com.ufs.core.repository.mapper.FileMetaMapper;
import com.ufs.core.service.MonitorService;
import com.ufs.core.tenant.TenantContextHolder;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 监控服务实现。生产数据源为 Prometheus + Micrometer（监控规范 §5）；
 * 此处基于元数据聚合 + 确定性合成时间序列做骨架演示，便于联调控制台三图。
 */
@Service
public class MonitorServiceImpl implements MonitorService {

    private final FileMetaMapper mapper;

    public MonitorServiceImpl(FileMetaMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public MonitorResponse get() {
        String tenant = TenantContextHolder.getTenantId();
        long totalBytes = mapper.sumBytes(tenant);

        List<MonitorResponse.UsagePoint> usage = new ArrayList<>();
        long base = totalBytes / 12;
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            long ts = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long total = base * (12 - i);
            usage.add(new MonitorResponse.UsagePoint(ym.toString(), total, base));
        }

        List<MonitorResponse.LatencyPoint> latency = new ArrayList<>();
        List<MonitorResponse.RequestPoint> requests = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 29; i >= 0; i--) {
            long ts = now - i * 60_000L;
            int p95 = 240 + (i % 5) * 12;
            latency.add(new MonitorResponse.LatencyPoint(ts, 110 + (i % 3) * 8, p95, p95 + 120));
            requests.add(new MonitorResponse.RequestPoint(ts, 800 + (i % 7) * 40L, 0.002 + (i % 4) * 0.001));
        }
        return new MonitorResponse(latency, usage, requests);
    }
}
