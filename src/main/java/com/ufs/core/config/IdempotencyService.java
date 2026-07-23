package com.ufs.core.config;

import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 幂等去重：基于 KeyValueStore 的 SET NX。首次返回 true，重复返回 false（对齐 Java 规范 §8）。
 */
@Service
public class IdempotencyService {

    private final KeyValueStore kvStore;

    public IdempotencyService(KeyValueStore kvStore) {
        this.kvStore = kvStore;
    }

    /** @return true 表示首次请求，可继续；false 表示重复请求。 */
    public boolean claim(String key, int ttlSeconds) {
        return kvStore.setIfAbsent("ufs:idem:" + key, "1", Duration.ofSeconds(ttlSeconds));
    }
}
