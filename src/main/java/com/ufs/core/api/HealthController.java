package com.ufs.core.api;

import com.ufs.core.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查（排除在认证拦截器外，对齐 WebConfig）。
 */
@RestController
@RequestMapping("/v1/health")
@Tag(name = "Health", description = "健康检查")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }
}
