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

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"spring.profiles.active=dev-local", "server.port=0"})
@AutoConfigureMockMvc
@ActiveProfiles("dev-local")
@DisplayName("集成测试: Policy 端点")
class PolicyControllerIntegrationTest {

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

    @Test
    @DisplayName("GET /v1/policies → 200 + 空列表（初始状态）")
    void listPoliciesReturnsEmpty() throws Exception {
        mockMvc.perform(get("/v1/policies")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /v1/policies → 200 + 创建策略成功")
    void createPolicySucceeds() throws Exception {
        String body = """
            {"bizType":"avatar","provider":"local","bucket":"ufs-demo-avatar",
             "maxFileSize":5242880,"allowedTypes":["image/png","image/jpeg"],
             "lifecycleDays":30,"pathTemplate":"avatar/{uuid}"}""";

        mockMvc.perform(post("/v1/policies")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.policyId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.bizType").value("avatar"))
                .andExpect(jsonPath("$.data.bucket").value("ufs-demo-avatar"))
                .andExpect(jsonPath("$.data.maxFileSize").value(5242880));
    }

    @Test
    @DisplayName("POST /v1/policies 缺少 bizType → 400")
    void createPolicyMissingBizTypeReturns400() throws Exception {
        String body = "{\"provider\":\"local\",\"bucket\":\"ufs-demo-avatar\",\"maxFileSize\":5242880}";

        mockMvc.perform(post("/v1/policies")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CRUD 完整流程: 创建→查询→更新→删除")
    void policyCrudFlow() throws Exception {
        String createBody = """
            {"bizType":"doc","provider":"local","bucket":"ufs-demo-doc",
             "maxFileSize":10485760,"allowedTypes":["application/pdf"],
             "lifecycleDays":60}""";

        // 创建
        String createResp = mockMvc.perform(post("/v1/policies")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policyId", not(emptyOrNullString())))
                .andReturn().getResponse().getContentAsString();

        String policyId = com.jayway.jsonpath.JsonPath.read(createResp, "$.data.policyId");

        // 查询
        mockMvc.perform(get("/v1/policies/" + policyId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bizType").value("doc"));

        // 列表包含新策略
        mockMvc.perform(get("/v1/policies")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 更新
        String updateBody = "{\"lifecycleDays\":90,\"active\":true}";
        mockMvc.perform(put("/v1/policies/" + policyId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lifecycleDays").value(90))
                .andExpect(jsonPath("$.data.active").value(true));

        // 删除
        mockMvc.perform(delete("/v1/policies/" + policyId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // 确认已删除
        mockMvc.perform(get("/v1/policies/" + policyId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }
}
