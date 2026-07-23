package com.ufs.core.config;

import java.time.Duration;

/**
 * 键值存储抽象（对齐 Java 规范 §8 幂等 / 安全规范 nonce 防重放）。
 * 生产用 Redis，dev-local 用内存实现，切换 profile 即可降级。
 */
public interface KeyValueStore {

    /**
     * 仅当 key 不存在时写入，带 TTL。
     * @return true=首次写入成功; false=key 已存在
     */
    boolean setIfAbsent(String key, String value, Duration ttl);
}
