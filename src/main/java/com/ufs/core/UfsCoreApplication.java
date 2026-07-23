package com.ufs.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * UFS 公共文件服务核心启动类。
 * 包扫描根：com.ufs.core（对齐《UFS_Java编码规范.md》§2 分层包结构）。
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.ufs.core.repository.mapper")
public class UfsCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(UfsCoreApplication.class, args);
    }
}
