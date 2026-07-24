package com.ufs.core.api;

import com.ufs.core.common.ApiResponse;
import com.ufs.core.model.dto.monitor.MonitorResponse;
import com.ufs.core.service.MonitorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监控接口（对齐 OpenAPI Monitor tag）：产出驱动控制台三图的指标。
 */
@RestController
@RequestMapping("/v1/monitor")
@Tag(name = "Monitor", description = "监控指标")
public class MonitorController {

    private final MonitorService service;

    public MonitorController(MonitorService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<MonitorResponse> get() {
        return ApiResponse.ok(service.get());
    }
}
