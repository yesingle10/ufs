package com.ufs.core.model.dto.monitor;

import java.util.List;

/**
 * 监控响应 DTO（对齐控制台三图：延迟趋势 / 存储量 / 请求量；监控规范 §5）。
 * 生产数据源为 Prometheus + Micrometer；此处 service 聚合元数据 + 指标注册表。
 */
public record MonitorResponse(
        List<LatencyPoint> latency,
        List<UsagePoint> usage,
        List<RequestPoint> requests) {

    public record LatencyPoint(long ts, int p50, int p95, int p99) {
    }

    public record UsagePoint(String month, long totalBytes, long deltaBytes) {
    }

    public record RequestPoint(long ts, long qps, double errorRatio) {
    }
}
