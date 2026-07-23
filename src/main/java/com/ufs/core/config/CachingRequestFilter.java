package com.ufs.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * 请求体缓存过滤器：包装 request 使 body 可重复读取（HMAC 签名校验需读 body，且不影响 Controller 解析）。
 * 必须在认证拦截器之前执行（Order 最高优先级）。
 */
@Component
@Order(1)
public class CachingRequestFilter extends org.springframework.web.filter.OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest wrapped = (request instanceof ContentCachingRequestWrapper)
                ? request : new ContentCachingRequestWrapper(request);
        chain.doFilter(wrapped, response);
    }
}
