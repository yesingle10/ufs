package com.ufs.core.service;

import com.ufs.core.model.dto.policy.CreatePolicyRequest;
import com.ufs.core.model.dto.policy.PolicyDto;
import com.ufs.core.model.dto.policy.UpdatePolicyRequest;

import java.util.List;

/**
 * 存储策略服务（按 bizType 路由到厂商/Bucket，对齐 PRD §9 路由）。
 */
public interface PolicyService {

    List<PolicyDto> listActive();

    PolicyDto create(CreatePolicyRequest req);

    PolicyDto get(String policyId);

    PolicyDto update(String policyId, UpdatePolicyRequest req);

    void delete(String policyId);
}
