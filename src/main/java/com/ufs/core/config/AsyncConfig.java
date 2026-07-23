package com.ufs.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步线程池（对齐 Java 规范 §11：禁用 new Thread / Executors.newFixedThreadPool，统一用 Spring TaskExecutor）。
 * 线程名可观测：ufs-async-N。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("ufsTaskExecutor")
    public Executor ufsTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8);
        ex.setMaxPoolSize(32);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("ufs-async-");
        ex.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        ex.setTaskDecorator(new com.ufs.core.config.MdcTaskDecorator());
        ex.initialize();
        return ex;
    }
}
