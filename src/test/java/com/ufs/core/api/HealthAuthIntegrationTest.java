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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"spring.profiles.active=dev-local", "server.port=0"})
@AutoConfigureMockMvc
@ActiveProfiles("dev-local")
@DisplayName("集成测试: Health & Auth 端点（无认证）")
class HealthAuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        jwtToken = jwtService.issue("t_demo", "test-operator", "app_demo");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("GET /v1/health → 200 + status=UP")
    void healthCheckReturnsUp() throws Exception {
        mockMvc.perform(get("/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("POST /v1/auth/token → 200 + 返回 JWT token")
    void issueTokenReturnsJwt() throws Exception {
        String body = "{\"appKey\":\"app_demo\",\"appSecret\":\"secret_demo\"}";

        mockMvc.perform(post("/v1/auth/token")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tenantId").value("t_demo"))
                .andExpect(jsonPath("$.data.expiresAt").isNumber());
    }

    @Test
    @DisplayName("POST /v1/auth/token → 401 错误凭证")
    void issueTokenWithBadCredentials() throws Exception {
        String body = "{\"appKey\":\"app_demo\",\"appSecret\":\"wrong_secret\"}";

        mockMvc.perform(post("/v1/auth/token")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("无认证访问受保护端点 → 401")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/v1/buckets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("错误 JWT 访问受保护端点 → 401")
    void badJwtReturns401() throws Exception {
        mockMvc.perform(get("/v1/buckets")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("有效 JWT 访问受保护端点 → 200")
    void validJwtAllowsAccess() throws Exception {
        mockMvc.perform(get("/v1/buckets")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));
    }
}
