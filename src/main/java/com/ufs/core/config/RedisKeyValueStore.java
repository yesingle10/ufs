package com.ufs.core.config;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 实现（生产 / dev profile 默认生效；dev-local 降级为内存）。
 */
@Component
@Primary
@Profile("!dev-local")
public class RedisKeyValueStore implements KeyValueStore {

    private final StringRedisTemplate redis;

    public RedisKeyValueStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, ttl));
    }
}
