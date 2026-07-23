package com.ufs.core.common.util;

import java.util.UUID;

/**
 * 业务 ID 生成器。产出 32 位字符串（对齐 PRD §8 的 VARCHAR(32) 业务主键）。
 * 生产可替换为雪花算法；此处用 UUID 去横线，保证无分库分表冲突。
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 带业务前缀的 ID，便于排查（如 f_ / u_ / p_）。 */
    public static String newId(String prefix) {
        return prefix + newId();
    }
}
