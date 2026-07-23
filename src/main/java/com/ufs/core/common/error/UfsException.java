package com.ufs.core.common.error;

import java.util.Map;

/**
 * UFS 业务异常。由 GlobalExceptionHandler 包成统一信封。
 */
public class UfsException extends RuntimeException {

    private final transient ErrorCode errorCode;
    private final transient Map<String, Object> data;

    public UfsException(ErrorCode errorCode) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.data = Map.of();
    }

    public UfsException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
        this.data = Map.of();
    }

    public UfsException(ErrorCode errorCode, Map<String, Object> data) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.data = data == null ? Map.of() : data;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
