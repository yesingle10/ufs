package com.ufs.core.service;

import com.ufs.core.model.dto.monitor.MonitorResponse;

/**
 * 监控服务：产出驱动控制台三图的指标（延迟趋势 / 存储量 / 请求量，对齐监控规范 §5）。
 */
public interface MonitorService {

    MonitorResponse get();
}
