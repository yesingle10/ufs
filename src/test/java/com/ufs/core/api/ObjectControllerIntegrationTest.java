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
@DisplayName("集成测试: Object 端点")
class ObjectControllerIntegrationTest {

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
    @DisplayName("POST /v1/objects/presign 简单上传 → 200 + 预签名 URL")
    void presignSimpleUploadSucceeds() throws Exception {
        String body = """
            {"bucket":"ufs-demo-avatar","objectKey":"test/photo.jpg",
             "fileName":"photo.jpg","fileSize":102400,"bizType":"avatar",
             "contentType":"image/jpeg"}""";

        mockMvc.perform(post("/v1/objects/presign")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.fileId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.method").value("PUT"))
                .andExpect(jsonPath("$.data.expiresAt").isNumber());
    }

    @Test
    @DisplayName("POST /v1/objects/presign 分片上传 → 200 + uploadId + 分片 URL")
    void presignMultipartUploadSucceeds() throws Exception {
        String body = """
            {"bucket":"ufs-demo-avatar","objectKey":"big/file.dat",
             "fileName":"file.dat","fileSize":50000000,"bizType":"avatar",
             "multipart":true,"partSize":10485760}""";

        mockMvc.perform(post("/v1/objects/presign")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.uploadId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.partUrls", aMapWithSize(greaterThan(0))));
    }

    @Test
    @DisplayName("POST /v1/objects/presign 缺少 fileName → 400")
    void presignMissingFileNameReturns400() throws Exception {
        String body = """
            {"bucket":"ufs-demo-avatar","fileSize":102400,"bizType":"avatar"}""";

        mockMvc.perform(post("/v1/objects/presign")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/objects → 200 + 游标分页")
    void listObjectsReturnsPage() throws Exception {
        mockMvc.perform(get("/v1/objects")
                        .header("Authorization", "Bearer " + jwt)
                        .param("bizType", "avatar")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.hasMore").isBoolean());
    }

    @Test
    @DisplayName("GET /v1/objects/head 不存在 → 404")
    void headMissingObjectReturns404() throws Exception {
        mockMvc.perform(get("/v1/objects/head")
                        .header("Authorization", "Bearer " + jwt)
                        .param("objectKey", "nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /v1/objects 不存在 → 404")
    void deleteMissingObjectReturns404() throws Exception {
        mockMvc.perform(delete("/v1/objects")
                        .header("Authorization", "Bearer " + jwt)
                        .param("objectKey", "nonexistent"))
                .andExpect(status().isNotFound());
    }
}
