package com.ufs.core.auth;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.config.properties.UfsProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 工具（HS256）。前端/JWT 模式鉴权（对齐契约 §3、安全规范 §1）。
 * 密钥来自配置（生产由 KMS 注入），绝不落库/日志。
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(UfsProperties props) {
        if (props.getJwtSecret() == null || props.getJwtSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ufs.jwt-secret 必须 ≥ 32 字节（来自 KMS）");
        }
        this.key = Keys.hmacShaKeyFor(props.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = props.getJwtTtlSeconds();
    }

    public String issue(String tenantId, String operatorId, String appKey) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(operatorId)
                .claim("tenantId", tenantId)
                .claim("appKey", appKey)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public JwtClaims parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new JwtClaims(
                    c.get("tenantId", String.class),
                    c.getSubject(),
                    c.get("appKey", String.class));
        } catch (JwtException e) {
            throw new UfsException(UfsErrorCode.UNAUTHORIZED, "invalid jwt");
        }
    }

    public record JwtClaims(String tenantId, String operatorId, String appKey) {
    }
}
