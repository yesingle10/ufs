package com.ufs.core.common.error;

import com.ufs.core.common.ApiResponse;
import com.ufs.core.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器：所有异常统一包成契约信封 ApiResponse。
 * 对齐《UFS_Java编码规范.md》§5 与契约 §4.1 / §5。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private String reqId() {
        String id = TenantContextHolder.getRequestId();
        return id != null ? id : MDC.get("requestId");
    }

    private String i18n(String messageKey) {
        try {
            return messageSource.getMessage(messageKey, null, messageKey, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return messageKey;
        }
    }

    @ExceptionHandler(UfsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUfs(UfsException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("ufs-biz code={} msg={}", code.getCode(), e.getMessage());
        ApiResponse<Object> body = ApiResponse.fail(code.getCode(), i18n(code.getMessageKey()), reqId(), e.getData());
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }

    @ExceptionHandler(VendorException.class)
    public ResponseEntity<ApiResponse<Object>> handleVendor(VendorException e) {
        ErrorCode code = e.toUfsErrorCode();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("vendor", e.getVendor());
        data.put("vendorCode", e.getVendorCode());
        log.error("ufs-vendor code={} vendor={} vcode={}", code.getCode(), e.getVendor(), e.getVendorCode());
        ApiResponse<Object> body = ApiResponse.fail(code.getCode(), i18n(code.getMessageKey()), reqId(), data);
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValid(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("ufs-valid fields={}", fields);
        ApiResponse<Object> body = ApiResponse.fail(UfsErrorCode.BAD_REQUEST.getCode(),
                i18n(UfsErrorCode.BAD_REQUEST.getMessageKey()), reqId(), fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleBody() {
        ApiResponse<Object> body = ApiResponse.fail(UfsErrorCode.BAD_REQUEST.getCode(),
                i18n(UfsErrorCode.BAD_REQUEST.getMessageKey()), reqId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleOther(Exception e) {
        log.error("ufs-unexpected", e);
        ApiResponse<Object> body = ApiResponse.fail(UfsErrorCode.INTERNAL.getCode(),
                i18n(UfsErrorCode.INTERNAL.getMessageKey()), reqId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
