package com.ufs.core.common.error;

/**
 * UFS 业务错误码，对齐《UFS接口契约》§5。
 * 调用方只依赖本枚举的 code，不依赖厂商私有码（厂商码经 VendorException 透传为 UFS.5020/5040）。
 * 框架无关：HTTP 状态以 int 表示，不引入 Spring 依赖。
 */
public enum UfsErrorCode implements ErrorCode {

    BAD_REQUEST("UFS.4000", 400, "Bad Request", "ufs.error.bad_request"),
    UNAUTHORIZED("UFS.4010", 401, "Unauthorized", "ufs.error.unauthorized"),
    FORBIDDEN("UFS.4030", 403, "Forbidden", "ufs.error.forbidden"),
    NOT_FOUND("UFS.4040", 404, "Not Found", "ufs.error.not_found"),
    CONFLICT("UFS.4090", 409, "Conflict", "ufs.error.conflict"),
    PAYLOAD_TOO_LARGE("UFS.4130", 413, "Payload Too Large", "ufs.error.payload_too_large"),
    UNSUPPORTED_MEDIA("UFS.4220", 422, "Unprocessable Entity", "ufs.error.unsupported_media"),
    RATE_LIMITED("UFS.4290", 429, "Too Many Requests", "ufs.error.rate_limited"),
    INTERNAL("UFS.5000", 500, "Internal Server Error", "ufs.error.internal"),
    VENDOR_ERROR("UFS.5020", 502, "Bad Gateway", "ufs.error.vendor_error"),
    VENDOR_TIMEOUT("UFS.5040", 504, "Gateway Timeout", "ufs.error.vendor_timeout"),
    CRR_DEGRADED("UFS.5041", 200, "OK", "ufs.error.crr_degraded");

    private final String code;
    private final int httpStatus;
    private final String reasonPhrase;
    private final String messageKey;

    UfsErrorCode(String code, int httpStatus, String reasonPhrase, String messageKey) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.reasonPhrase = reasonPhrase;
        this.messageKey = messageKey;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
