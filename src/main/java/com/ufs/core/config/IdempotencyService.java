package com.ufs.core.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 幂等去重：基于 Redis 的 SET NX。首次返回 true，重复返回 false（对齐 Java 规范 §8）。
 */
@Service
public class IdempotencyService {

    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** @return true 表示首次请求，可继续；false 表示重复请求。 */
    public boolean claim(String key, int ttlSeconds) {
        return Boolean.TRUE.equals(redis.opsForValue()
                .setIfAbsent("ufs:idem:" + key, "1", Duration.ofSeconds(ttlSeconds)));
    }
}
