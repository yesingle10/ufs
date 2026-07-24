package com.ufs.core.api;

import com.ufs.core.common.ApiResponse;
import com.ufs.core.common.PageResult;
import com.ufs.core.model.dto.audit.AuditLogDto;
import com.ufs.core.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计接口（对齐 OpenAPI Audit tag）：检索审计日志。
 */
@RestController
@RequestMapping("/v1/audit")
@Tag(name = "Audit", description = "审计日志")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<AuditLogDto>> list(@RequestParam(required = false) String operatorId,
                                                     @RequestParam(required = false) String action,
                                                     @RequestParam(required = false) String cursor,
                                                     @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(service.list(operatorId, action, cursor, limit));
    }
}
