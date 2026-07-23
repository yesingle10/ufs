package com.ufs.core.config;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 幂等拦截器：对标记 {@link Idempotent} 的写操作，按 Idempotency-Key 做 Redis 去重。
 * 重复请求直接拒绝（UFS.4090），避免双写（对齐 Java 规范 §8、契约 §4.4）。
 * 注：生产可进一步缓存首次响应体以原样回放；此处以“拒绝重复”保证安全。
 */
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyService idempotencyService;

    public IdempotencyInterceptor(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        Idempotent ann = hm.getMethodAnnotation(Idempotent.class);
        if (ann == null) {
            return true;
        }
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            return true; // 未携带幂等键则不作保护（由调用方决定）
        }
        String redisKey = TenantContextHolder.getTenantId() + ":" + key;
        if (!idempotencyService.claim(redisKey, ann.ttlSeconds())) {
            throw new UfsException(UfsErrorCode.CONFLICT, "duplicate request (idempotency-key)");
        }
        return true;
    }
}
