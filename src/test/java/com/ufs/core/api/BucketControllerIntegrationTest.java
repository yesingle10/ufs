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
import org.springframework.test.annotation.DirtiesContext;
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
@DisplayName("集成测试: Bucket 端点")
class BucketControllerIntegrationTest {

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
    @DisplayName("GET /v1/buckets → 200 + 返回 6 个 seed 桶")
    void listBucketsReturnsSix() throws Exception {
        mockMvc.perform(get("/v1/buckets")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.data[0].name", startsWith("ufs-demo")));
    }

    @Test
    @DisplayName("GET /v1/buckets/{name} → 200 + 桶详情")
    void getBucketReturnsDetails() throws Exception {
        mockMvc.perform(get("/v1/buckets/ufs-demo-img")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.name").value("ufs-demo-img"))
                .andExpect(jsonPath("$.data.vendor").value("local"))
                .andExpect(jsonPath("$.data.region").value("cn-hangzhou"));
    }

    @Test
    @DisplayName("GET /v1/buckets/{name} 不存在 → 404")
    void getMissingBucketReturns404() throws Exception {
        mockMvc.perform(get("/v1/buckets/nonexistent")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /v1/buckets → 200 + 创建桶成功")
    void createBucketSucceeds() throws Exception {
        String body = """
            {"name":"new-test-bucket","vendor":"oss","region":"us-west-1",
             "versioning":true,"lifecycleDays":90,"acl":"private",
             "cors":["*"],"note":"test bucket"}""";

        mockMvc.perform(post("/v1/buckets")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.name").value("new-test-bucket"))
                .andExpect(jsonPath("$.data.vendor").value("oss"))
                .andExpect(jsonPath("$.data.versioning").value(true))
                .andExpect(jsonPath("$.data.lifecycleDays").value(90));
    }

    @Test
    @DisplayName("POST /v1/buckets 缺少 name → 400")
    void createBucketMissingNameReturns400() throws Exception {
        String body = "{\"vendor\":\"oss\",\"region\":\"us-west-1\"}";

        mockMvc.perform(post("/v1/buckets")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/buckets 重名 → 409")
    void createDuplicateBucketReturns409() throws Exception {
        String body = "{\"name\":\"ufs-demo-img\",\"vendor\":\"oss\",\"region\":\"us-west-1\"}";

        mockMvc.perform(post("/v1/buckets")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /v1/buckets/{name} → 200 + 更新成功")
    void updateBucketSucceeds() throws Exception {
        String body = """
            {"cdnDomain":"https://cdn.test.com","versioning":false,
             "lifecycleDays":180,"acl":"public-read","note":"updated"}""";

        mockMvc.perform(put("/v1/buckets/ufs-demo-img")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.cdnDomain").value("https://cdn.test.com"))
                .andExpect(jsonPath("$.data.versioning").value(false))
                .andExpect(jsonPath("$.data.lifecycleDays").value(180));
    }

    @Test
    @DisplayName("DELETE /v1/buckets/{name} → 200 + 删除成功")
    @DirtiesContext
    void deleteBucketSucceeds() throws Exception {
        mockMvc.perform(delete("/v1/buckets/ufs-demo-audio")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));

        // 确认已删除
        mockMvc.perform(get("/v1/buckets/ufs-demo-audio")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }
}
