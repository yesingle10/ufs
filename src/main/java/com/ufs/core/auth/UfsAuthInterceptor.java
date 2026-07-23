package com.ufs.core.auth;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.config.properties.UfsProperties;
import com.ufs.core.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * 统一认证拦截器：支持 HMAC-SHA256 签名（服务端）与 Bearer JWT（前端）双模式（对齐契约 §3）。
 * 校验通过后写入 {@link TenantContextHolder} 与 MDC（requestId/tenantId），供全链路使用。
 */
@Component
public class UfsAuthInterceptor implements HandlerInterceptor {

    private static final String SCHEMA_HMAC = "UFS ";
    private static final String SCHEMA_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final CredentialResolver credentialResolver;
    private final StringRedisTemplate redis;
    private final UfsProperties props;

    public UfsAuthInterceptor(JwtService jwtService, CredentialResolver credentialResolver,
                             StringRedisTemplate redis, UfsProperties props) {
        this.jwtService = jwtService;
        this.credentialResolver = credentialResolver;
        this.redis = redis;
        this.props = props;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String requestId = firstNonBlank(request.getHeader("X-Request-Id"), UUID.randomUUID().toString());
        MDC.put("requestId", requestId);

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            throw new UfsException(UfsErrorCode.UNAUTHORIZED, "missing authorization");
        }
        if (auth.startsWith(SCHEMA_BEARER)) {
            handleJwt(auth.substring(SCHEMA_BEARER.length()).trim(), requestId);
        } else if (auth.startsWith(SCHEMA_HMAC)) {
            handleHmac(auth.substring(SCHEMA_HMAC.length()).trim(), request, requestId);
        } else {
            throw new UfsException(UfsErrorCode.UNAUTHORIZED, "unsupported auth schema");
        }
        return true;
    }

    private void handleJwt(String token, String requestId) {
        JwtService.JwtClaims c = jwtService.parse(token);
        TenantContextHolder.set(TenantContextHolder.Context.of(c.tenantId(), c.operatorId(), requestId, c.appKey()));
        MDC.put("tenantId", c.tenantId());
    }

    private void handleHmac(String auth, HttpServletRequest request, String requestId) {
        int idx = auth.indexOf(':');
        if (idx < 0) {
            throw new UfsException(UfsErrorCode.UNAUTHORIZED, "malformed hmac auth");
        }
        String appKey = auth.substring(0, idx);
        String signature = auth.substring(idx + 1);

        String tsHeader = request.getHeader("X-Ufs-Timestamp");
        String nonce = request.getHeader("X-Ufs-Nonce");
        if (tsHeader == null || nonce == null) {
            throw new UfsException(UfsErrorCode.UNAUTHORIZED, "missing timestamp/nonce");
        }
        long ts = Long.parseLong(tsHeader);
        long skew = Math.abs(System.currentTimeMillis() - ts * 1000);
        if (skew > props.getSignTtlSeconds() * 1000) {
            throw new UfsException(UfsErrorCode.UNAUTHORIZED, "timestamp skew too large");
        }
        if (Boolean.FALSE.equals(redis.opsForValue().setIfAbsent("ufs:nonce:" + nonce, "1",
                Duration.ofSeconds(props.getNonceTtlSeconds())))) {
            throw new UfsException(UfsErrorCode.RATE_LIMITED, "replay detected");
        }

        AppCredential cred = credentialResolver.resolve(appKey);
        String body = readBody(request);
        String canonical = HmacSigner.canonical(request.getMethod(), request.getRequestURI(),
                ts, nonce, body);
        if (!HmacSigner.verify(cred.appSecret(), canonical, signature)) {
            throw new UfsException(UfsErrorCode.UNAUTHORIZED, "signature mismatch");
        }
        TenantContextHolder.set(TenantContextHolder.Context.of(cred.tenantId(), appKey, requestId, appKey));
        MDC.put("tenantId", cred.tenantId());
    }

    private String readBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper w) {
            try {
                w.getInputStream().readAllBytes(); // 触发缓存
            } catch (java.io.IOException ignored) {
                // 缓存触发失败不影响后续 getContentAsByteArray
            }
            byte[] bytes = w.getContentAsByteArray();
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return "";
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        TenantContextHolder.clear();
        MDC.remove("requestId");
        MDC.remove("tenantId");
    }

    private static String firstNonBlank(String a, String b) {
        return (a == null || a.isBlank()) ? b : a;
    }
}
