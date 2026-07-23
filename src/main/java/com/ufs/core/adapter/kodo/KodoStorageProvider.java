package com.ufs.core.adapter.kodo;

import com.ufs.core.adapter.AbstractVendorStorageProvider;
import org.springframework.stereotype.Component;

/**
 * 七牛云 Kodo 适配器（占位）。生产引入 qiniu-java-sdk，实现分片/预签名/CDN 回源等。
 */
@Component
public class KodoStorageProvider extends AbstractVendorStorageProvider {

    @Override
    public String vendor() {
        return "kodo";
    }
}
