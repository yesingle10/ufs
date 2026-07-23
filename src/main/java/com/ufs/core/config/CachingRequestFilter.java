package com.ufs.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 请求体缓存过滤器：将 body 全量读入内存，支持多次读取（HMAC 签名校验 + Controller @RequestBody）。
 * 必须在认证拦截器之前执行（Order 最高优先级）。
 */
@Component
@Order(1)
public class CachingRequestFilter extends org.springframework.web.filter.OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CachedBodyHttpServletRequest wrapped = (request instanceof CachedBodyHttpServletRequest)
                ? (CachedBodyHttpServletRequest) request : new CachedBodyHttpServletRequest(request);
        chain.doFilter(wrapped, response);
    }

    /**
     * 可重复读取 body 的 request 包装。
     */
    public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        public byte[] getCachedBody() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedServletInputStream(cachedBody);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(cachedBody), StandardCharsets.UTF_8));
        }

        private static class CachedServletInputStream extends ServletInputStream {
            private final ByteArrayInputStream delegate;

            CachedServletInputStream(byte[] body) {
                this.delegate = new ByteArrayInputStream(body);
            }

            @Override
            public boolean isFinished() {
                return delegate.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return delegate.read();
            }
        }
    }
}
