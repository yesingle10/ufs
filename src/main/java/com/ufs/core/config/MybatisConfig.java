package com.ufs.core.config;

import com.ufs.core.tenant.TenantSqlInterceptor;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置：注册租户守卫拦截器（对齐 SQL 规范 §7），并开启下划线→驼峰映射。
 */
@Configuration
public class MybatisConfig {

    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer(TenantSqlInterceptor tenantSqlInterceptor) {
        return configuration -> {
            configuration.addInterceptor(tenantSqlInterceptor);
            configuration.setMapUnderscoreToCamelCase(true);
        };
    }
}
