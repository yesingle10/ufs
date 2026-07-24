package com.ufs.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 配置 — Swagger UI + JWT 鉴权方案。
 *
 * <p>访问路径: /swagger-ui/index.html</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ufsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UFS — 统一文件服务 API")
                        .description("""
                                UFS (Unified File Service) 公共文件服务。
                                
                                ## 鉴权方式
                                1. 调用 **POST /v1/auth/token** 获取 JWT
                                2. 点击右上角 **Authorize** 按钮，粘贴 `Bearer <token>`
                                3. 后续请求自动携带 JWT
                                
                                ## 租户隔离
                                所有数据操作按 `tenantId` 隔离，JWT payload 中携带租户信息。
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("UFS Team").email("dev@ufs.io"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("本地开发"),
                        new Server().url("/").description("同源部署（Docker / K8s）")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .name("Bearer")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入 JWT token（不含 Bearer 前缀则由 Swagger 自动添加）")));
    }
}
