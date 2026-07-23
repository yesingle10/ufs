package com.ufs.core.common.error;

/**
 * 错误码契约：每一个错误码绑定 HTTP 状态(int)与 i18n messageKey。
 * 设计为框架无关（不依赖 Spring），便于在 core 层独立测试与编译校验。
 * 实现枚举（如 UfsErrorCode）必须与《UFS接口契约》§5 一一对应。
 */
public interface ErrorCode {

    String getCode();

    /** HTTP 状态码（RFC 7231）。 */
    int getHttpStatus();

    /** HTTP 原因短语，例如 "Bad Request"，用于日志与可观测性。 */
    String getReasonPhrase();

    String getMessageKey();
}
