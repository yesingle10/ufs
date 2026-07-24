package com.ufs.core.api;

import com.ufs.core.auth.AppCredential;
import com.ufs.core.auth.CredentialResolver;
import com.ufs.core.auth.JwtService;
import com.ufs.core.common.ApiResponse;
import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.config.properties.UfsProperties;
import com.ufs.core.model.dto.auth.IssueTokenRequest;
import com.ufs.core.model.dto.auth.TokenResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权端点（排除在认证拦截器外，自身用 appKey+appSecret 校验后签发 JWT）。
 * 对齐契约 §3 双模式：HMAC 用于服务端全能力，JWT 用于前端只读/临时任务。
 */
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Auth", description = "JWT 鉴权")
public class AuthController {

    private final CredentialResolver credentialResolver;
    private final JwtService jwtService;
    private final UfsProperties props;

    public AuthController(CredentialResolver credentialResolver, JwtService jwtService, UfsProperties props) {
        this.credentialResolver = credentialResolver;
        this.jwtService = jwtService;
        this.props = props;
    }

    @PostMapping("/token")
    public ApiResponse<TokenResponse> token(@Valid @RequestBody IssueTokenRequest req) {
        AppCredential cred = credentialResolver.resolve(req.appKey());
        if (!cred.appSecret().equals(req.appSecret())) {
            throw new UfsException(UfsErrorCode.UNAUTHORIZED, "bad appSecret");
        }
        String operator = req.operatorId() == null ? req.appKey() : req.operatorId();
        String token = jwtService.issue(cred.tenantId(), operator, req.appKey());
        long exp = System.currentTimeMillis() + props.getJwtTtlSeconds() * 1000;
        return ApiResponse.ok(new TokenResponse(token, exp, cred.tenantId()));
    }
}
