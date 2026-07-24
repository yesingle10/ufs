package com.ufs.core.config;

import com.ufs.core.auth.UfsAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置：注册认证拦截器与幂等拦截器（对齐 Java 规范 §4 / §8、契约 §3 / §4.4）。
 * 认证放行 /v1/auth/**（签发 JWT 的登录端点自身用 HMAC 校验）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UfsAuthInterceptor authInterceptor;
    private final IdempotencyInterceptor idempotencyInterceptor;

    public WebConfig(UfsAuthInterceptor authInterceptor, IdempotencyInterceptor idempotencyInterceptor) {
        this.authInterceptor = authInterceptor;
        this.idempotencyInterceptor = idempotencyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns("/v1/auth/**", "/v1/health/**", "/error",
                        "/swagger-ui/**", "/v3/api-docs/**");
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/v1/**");
    }
}
