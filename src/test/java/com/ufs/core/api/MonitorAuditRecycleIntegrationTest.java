package com.ufs.core.api;

import com.ufs.core.auth.JwtService;
import com.ufs.core.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"spring.profiles.active=dev-local", "server.port=0"})
@AutoConfigureMockMvc
@ActiveProfiles("dev-local")
@DisplayName("集成测试: Monitor / Audit / Recycle 端点")
class MonitorAuditRecycleIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    private String jwt;

    @BeforeEach
    void setUp() {
        jwt = jwtService.issue("t_demo", "test-op", "app_demo");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ── Monitor ───────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/monitor → 200 + 三图数据")
    void monitorReturnsThreeSeries() throws Exception {
        mockMvc.perform(get("/v1/monitor")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.latency", hasSize(30)))
                .andExpect(jsonPath("$.data.requests", hasSize(30)))
                .andExpect(jsonPath("$.data.usage", hasSize(12)))
                .andExpect(jsonPath("$.data.latency[0].p50", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.requests[0].qps", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.usage[0].totalBytes", greaterThanOrEqualTo(0)));
    }

    // ── Audit ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/audit → 200 + 分页结果")
    void auditReturnsPage() throws Exception {
        mockMvc.perform(get("/v1/audit")
                        .header("Authorization", "Bearer " + jwt)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.pageSize").isNumber())
                .andExpect(jsonPath("$.data.hasMore").isBoolean());
    }

    @Test
    @DisplayName("GET /v1/audit?operatorId=admin → 200 + 按操作者筛选")
    void auditFilterByOperator() throws Exception {
        mockMvc.perform(get("/v1/audit")
                        .header("Authorization", "Bearer " + jwt)
                        .param("operatorId", "admin")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));
    }

    // ── Recycle ───────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/recycle → 200 + 回收站列表")
    void recycleListReturnsPage() throws Exception {
        mockMvc.perform(get("/v1/recycle")
                        .header("Authorization", "Bearer " + jwt)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("POST /v1/recycle/restore 不存在触发 NOT_FOUND")
    void recycleRestoreMissing() throws Exception {
        mockMvc.perform(post("/v1/recycle/restore")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":\"nonexistent\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /v1/recycle/{fileId} 不存在触发 NOT_FOUND")
    void recyclePurgeMissing() throws Exception {
        mockMvc.perform(delete("/v1/recycle/nonexistent")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }
}
