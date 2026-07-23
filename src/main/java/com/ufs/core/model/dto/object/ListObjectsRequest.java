package com.ufs.core.model.dto.object;

/**
 * 列举对象请求（游标分页，对齐契约 §4.2 / SQL 规范 §6）。
 */
public record ListObjectsRequest(
        String bizType,
        String provider,
        Integer status,
        String cursor,
        int limit) {

    public static ListObjectsRequest of(String bizType, String provider, Integer status, String cursor, int limit) {
        return new ListObjectsRequest(bizType, provider, status, cursor == null ? String.valueOf(Long.MAX_VALUE) : cursor, limit);
    }
}
