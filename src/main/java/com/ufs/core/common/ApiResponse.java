package com.ufs.core.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * 统一响应信封，对齐《UFS接口契约与接入文档.md》§4.1。
 * 成功 code="0000"；业务失败 code="UFS.xxxx"（见 UfsErrorCode）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {

    private final String code;
    private final String message;
    private final String requestId;
    private final long timestamp;
    private final T data;

    private ApiResponse(String code, String message, String requestId, T data) {
        this.code = code;
        this.message = message;
        this.requestId = requestId;
        this.timestamp = Instant.now().toEpochMilli();
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>("0000", "success", requestId, data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, String requestId) {
        return new ApiResponse<>(code, message, requestId, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, String requestId, T data) {
        return new ApiResponse<>(code, message, requestId, data);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public T getData() {
        return data;
    }
}
