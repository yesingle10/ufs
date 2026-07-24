package com.ufs.core.api;

import com.ufs.core.common.ApiResponse;
import com.ufs.core.config.Idempotent;
import com.ufs.core.model.dto.policy.CreatePolicyRequest;
import com.ufs.core.model.dto.policy.PolicyDto;
import com.ufs.core.model.dto.policy.UpdatePolicyRequest;
import com.ufs.core.service.PolicyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 存储策略接口（对齐 OpenAPI Policy tag）：按 bizType 路由到厂商/Bucket。
 */
@RestController
@RequestMapping("/v1/policies")
@Tag(name = "Policies", description = "存储策略管理")
public class PolicyController {

    private final PolicyService service;

    public PolicyController(PolicyService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<PolicyDto>> list() {
        return ApiResponse.ok(service.listActive());
    }

    @GetMapping("/{policyId}")
    public ApiResponse<PolicyDto> get(@PathVariable String policyId) {
        return ApiResponse.ok(service.get(policyId));
    }

    @PostMapping
    @Idempotent
    public ApiResponse<PolicyDto> create(@Valid @RequestBody CreatePolicyRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/{policyId}")
    public ApiResponse<PolicyDto> update(@PathVariable String policyId, @Valid @RequestBody UpdatePolicyRequest req) {
        return ApiResponse.ok(service.update(policyId, req));
    }

    @DeleteMapping("/{policyId}")
    public ApiResponse<Void> delete(@PathVariable String policyId) {
        service.delete(policyId);
        return ApiResponse.ok();
    }
}
