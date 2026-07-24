package com.ufs.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ufs.core.audit.AuditEvent;
import com.ufs.core.audit.AuditPublisher;
import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.model.dto.policy.CreatePolicyRequest;
import com.ufs.core.model.dto.policy.PolicyDto;
import com.ufs.core.model.dto.policy.UpdatePolicyRequest;
import com.ufs.core.repository.entity.StoragePolicy;
import com.ufs.core.repository.mapper.StoragePolicyMapper;
import com.ufs.core.service.impl.PolicyServiceImpl;
import com.ufs.core.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PolicyService 单元测试")
@ExtendWith(MockitoExtension.class)
class PolicyServiceImplTest {

    @Mock private StoragePolicyMapper mapper;
    @Mock private AuditPublisher audit;
    @Mock private ObjectMapper json;

    private PolicyServiceImpl service;

    private static final String TENANT = "t_demo";
    private static final String OPERATOR = "test-op";

    @BeforeEach
    void setUp() {
        TenantContextHolder.set(new TenantContextHolder.Context(TENANT, OPERATOR, "req-01", "app_demo", false));
        service = new PolicyServiceImpl(mapper, audit, json);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ── listActive ────────────────────────────────────────

    @Test
    @DisplayName("listActive: 返回策略列表")
    void shouldReturnActivePolicies() throws Exception {
        StoragePolicy p = policy("p_001", "avatar", "local", "ufs-demo-avatar");
        when(mapper.listActive(TENANT)).thenReturn(List.of(p));
        when(json.readValue(eq("[\"image/png\"]"), any(TypeReference.class)))
                .thenReturn(List.of("image/png"));

        List<PolicyDto> result = service.listActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).policyId()).isEqualTo("p_001");
        assertThat(result.get(0).allowedTypes()).containsExactly("image/png");
        verify(mapper).listActive(TENANT);
    }

    @Test
    @DisplayName("listActive: 空结果")
    void shouldReturnEmptyListWhenNoPolicies() {
        when(mapper.listActive(TENANT)).thenReturn(List.of());

        List<PolicyDto> result = service.listActive();

        assertThat(result).isEmpty();
    }

    // ── get ───────────────────────────────────────────────

    @Test
    @DisplayName("get: 存在则返回策略")
    void shouldReturnPolicyWhenExists() throws Exception {
        StoragePolicy p = policy("p_001", "avatar", "local", "ufs-demo-avatar");
        when(mapper.selectById(TENANT, "p_001")).thenReturn(p);
        when(json.readValue(eq("[\"image/png\"]"), any(TypeReference.class)))
                .thenReturn(List.of("image/png"));

        PolicyDto result = service.get("p_001");

        assertThat(result.policyId()).isEqualTo("p_001");
        verify(mapper).selectById(TENANT, "p_001");
    }

    @Test
    @DisplayName("get: 不存在抛出 NOT_FOUND")
    void shouldThrowNotFoundWhenPolicyMissing() {
        when(mapper.selectById(TENANT, "p_none")).thenReturn(null);

        assertThatThrownBy(() -> service.get("p_none"))
                .isInstanceOf(UfsException.class)
                .matches(e -> ((UfsException) e).getErrorCode() == UfsErrorCode.NOT_FOUND);
    }

    // ── create ────────────────────────────────────────────

    @Test
    @DisplayName("create: 成功创建策略并发送审计事件")
    void shouldCreatePolicyAndPublishAudit() throws Exception {
        CreatePolicyRequest req = new CreatePolicyRequest(
                "avatar", "local", "ufs-demo-avatar",
                "avatar/{yyyy}/{MM}/{dd}/{uuid}", 5_242_880L,
                List.of("image/png", "image/jpeg"), 30, null
        );
        when(json.writeValueAsString(req.allowedTypes())).thenReturn("[\"image/png\",\"image/jpeg\"]");

        service.create(req);

        ArgumentCaptor<StoragePolicy> policyCaptor = ArgumentCaptor.forClass(StoragePolicy.class);
        verify(mapper).insert(policyCaptor.capture());
        StoragePolicy inserted = policyCaptor.getValue();
        assertThat(inserted.bizType()).isEqualTo("avatar");
        assertThat(inserted.maxFileSize()).isEqualTo(5_242_880L);
        assertThat(inserted.lifecycleDays()).isEqualTo(30);
        assertThat(inserted.policyId()).startsWith("p_");

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).publish(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("policy.create");
    }

    // ── update ────────────────────────────────────────────

    @Test
    @DisplayName("update: 成功更新策略")
    void shouldUpdatePolicyFields() throws Exception {
        StoragePolicy existing = policy("p_001", "avatar", "local", "ufs-demo-avatar");
        when(mapper.selectById(TENANT, "p_001")).thenReturn(existing);

        UpdatePolicyRequest req = new UpdatePolicyRequest(
                "oss", "ufs-demo-img", null, 10_485_760L,
                List.of("image/*"), 60, null, true
        );
        when(json.writeValueAsString(req.allowedTypes())).thenReturn("[\"image/*\"]");

        PolicyDto result = service.update("p_001", req);

        assertThat(result.provider()).isEqualTo("oss");
        assertThat(result.maxFileSize()).isEqualTo(10_485_760L);
        assertThat(result.lifecycleDays()).isEqualTo(60);
        assertThat(result.active()).isTrue();
        verify(mapper).setActive(eq(TENANT), eq("p_001"), eq(1), any(long.class));
        verify(audit).publish(any(AuditEvent.class));
    }

    @Test
    @DisplayName("update: 策略不存在抛出 NOT_FOUND")
    void shouldThrowNotFoundOnUpdateMissing() {
        when(mapper.selectById(TENANT, "p_none")).thenReturn(null);
        UpdatePolicyRequest req = new UpdatePolicyRequest(null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.update("p_none", req))
                .isInstanceOf(UfsException.class)
                .matches(e -> ((UfsException) e).getErrorCode() == UfsErrorCode.NOT_FOUND);
    }

    // ── delete ────────────────────────────────────────────

    @Test
    @DisplayName("delete: 成功删除策略并发送审计事件")
    void shouldDeletePolicyAndPublishAudit() {
        StoragePolicy existing = policy("p_001", "avatar", "local", "ufs-demo-avatar");
        when(mapper.selectById(TENANT, "p_001")).thenReturn(existing);

        service.delete("p_001");

        verify(mapper).delete(TENANT, "p_001");
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).publish(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("policy.delete");
    }

    @Test
    @DisplayName("delete: 策略不存在抛出 NOT_FOUND")
    void shouldThrowNotFoundOnDeleteMissing() {
        when(mapper.selectById(TENANT, "p_none")).thenReturn(null);

        assertThatThrownBy(() -> service.delete("p_none"))
                .isInstanceOf(UfsException.class)
                .matches(e -> ((UfsException) e).getErrorCode() == UfsErrorCode.NOT_FOUND);
    }

    // ── helper ────────────────────────────────────────────

    private StoragePolicy policy(String id, String bizType, String provider, String bucket) {
        return new StoragePolicy(
                id, TENANT, bizType, provider, bucket,
                "default/{uuid}", 10_485_760L, "[\"image/png\"]",
                30, null, 1, System.currentTimeMillis()
        );
    }
}
