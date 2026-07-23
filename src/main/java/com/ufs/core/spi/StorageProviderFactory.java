package com.ufs.core.spi;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 存储厂商路由工厂：按 vendor 路由到对应实现（配置驱动，对齐 Java 规范 §6、PRD 架构）。
 * Spring 收集所有 StorageProvider Bean，按 vendor() 建索引。
 */
@Component
public class StorageProviderFactory {

    private final Map<String, StorageProvider> providers;

    public StorageProviderFactory(List<StorageProvider> all) {
        this.providers = all.stream().collect(Collectors.toMap(StorageProvider::vendor, Function.identity()));
    }

    public StorageProvider get(String vendor) {
        StorageProvider p = providers.get(vendor);
        if (p == null) {
            throw new UfsException(UfsErrorCode.VENDOR_ERROR, Map.of("vendor", vendor));
        }
        return p;
    }
}
