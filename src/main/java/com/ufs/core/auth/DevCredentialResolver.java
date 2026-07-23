package com.ufs.core.auth;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.config.properties.UfsProperties;
import org.springframework.stereotype.Component;

/**
 * 开发期凭证解析：直接读配置（生产须替换为 KMS 实现，密钥不落库/不进 Git）。
 */
@Component
public class DevCredentialResolver implements CredentialResolver {

    private final UfsProperties props;

    public DevCredentialResolver(UfsProperties props) {
        this.props = props;
    }

    @Override
    public AppCredential resolve(String appKey) {
        UfsProperties.AppCredential c = props.getAppCredentials().get(appKey);
        if (c == null) {
            throw new UfsException(UfsErrorCode.UNAUTHORIZED, "unknown appKey");
        }
        return new AppCredential(c.tenantId(), c.appSecret());
    }
}
