package com.ufs.core.adapter.tos;

import com.ufs.core.adapter.AbstractVendorStorageProvider;
import org.springframework.stereotype.Component;

/**
 * 火山引擎 TOS 适配器（占位）。生产引入火山 TOS Java SDK，实现分片/预签名/CRR 等。
 */
@Component
public class TosStorageProvider extends AbstractVendorStorageProvider {

    @Override
    public String vendor() {
        return "tos";
    }
}
