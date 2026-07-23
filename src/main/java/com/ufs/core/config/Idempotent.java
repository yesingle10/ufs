package com.ufs.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记写操作需幂等保护。拦截器读取 Idempotency-Key 头做 Redis 去重（对齐 Java 规范 §8、契约 §4.4）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    int ttlSeconds() default 86400;
}
