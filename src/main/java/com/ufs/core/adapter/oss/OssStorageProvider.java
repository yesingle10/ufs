package com.ufs.core.adapter.oss;

import com.ufs.core.adapter.AbstractVendorStorageProvider;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 适配器（占位）。生产引入 aliyun-oss-sdk，实现分片/预签名/COPY/生命周期等。
 */
@Component
public class OssStorageProvider extends AbstractVendorStorageProvider {

    @Override
    public String vendor() {
        return "oss";
    }
}
