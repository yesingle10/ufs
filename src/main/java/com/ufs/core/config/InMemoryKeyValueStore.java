package com.ufs.core.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存实现（dev-local profile：无 Redis 时降级，功能等价但不持久）。
 */
@Component
@Profile("dev-local")
public class InMemoryKeyValueStore implements KeyValueStore {

    private record Entry(String value, long expiresAt) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        long now = System.currentTimeMillis();
        Entry existing = store.get(key);
        if (existing != null && existing.expiresAt > now) {
            return false; // key 仍有效
        }
        store.put(key, new Entry(value, now + ttl.toMillis()));
        return true;
    }
}
