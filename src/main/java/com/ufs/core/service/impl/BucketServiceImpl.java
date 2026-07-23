package com.ufs.core.service.impl;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.model.dto.bucket.BucketDto;
import com.ufs.core.model.dto.bucket.CreateBucketRequest;
import com.ufs.core.model.dto.bucket.UpdateBucketRequest;
import com.ufs.core.service.BucketService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储桶服务（内存注册表实现，演示/测试用；生产替换为 DB + 缓存，对齐 Java 规范 §12）。
 * dev 演示桶统一用 local 厂商，确保本地可直接联调对象上传/列举/下载。
 */
@Service
public class BucketServiceImpl implements BucketService {

    private final Map<String, BucketDto> registry = new ConcurrentHashMap<>();

    public BucketServiceImpl() {
        seed();
    }

    private void seed() {
        registry.put("ufs-demo-img", of("ufs-demo-img", "local", "cn-hangzhou",
                "https://img.demo.ufs.local", "AES256", true, false, 30, "private", "演示图片桶"));
        registry.put("ufs-demo-video", of("ufs-demo-video", "local", "cn-hangzhou",
                null, "AES256", true, false, 90, "private", "演示视频桶"));
        registry.put("ufs-demo-doc", of("ufs-demo-doc", "local", "cn-hangzhou",
                null, "AES256", false, false, 365, "private", "演示文档桶"));
        registry.put("ufs-demo-avatar", of("ufs-demo-avatar", "local", "cn-hangzhou",
                "https://avatar.demo.ufs.local", "AES256", true, false, 30, "public-read", "演示头像桶"));
        registry.put("ufs-demo-audio", of("ufs-demo-audio", "local", "cn-hangzhou",
                null, "AES256", false, false, 60, "private", "演示音频桶"));
        registry.put("ufs-demo-static", of("ufs-demo-static", "local", "cn-hangzhou",
                "https://static.demo.ufs.local", "AES256", true, false, 30, "public-read", "演示静态资源桶"));
    }

    private BucketDto of(String name, String vendor, String region, String cdn, String enc,
                         boolean ver, boolean crr, int life, String acl, String note) {
        return new BucketDto(name, vendor, region, cdn, enc, ver, crr, life, acl, List.of("*"), note, 0L, 0L);
    }

    @Override
    public List<BucketDto> list() {
        return new ArrayList<>(registry.values());
    }

    @Override
    public BucketDto get(String name) {
        BucketDto b = registry.get(name);
        if (b == null) {
            throw new UfsException(UfsErrorCode.NOT_FOUND, Map.of("bucket", name));
        }
        return b;
    }

    @Override
    public BucketDto create(CreateBucketRequest req) {
        if (registry.containsKey(req.name())) {
            throw new UfsException(UfsErrorCode.CONFLICT, Map.of("bucket", req.name()));
        }
        BucketDto b = new BucketDto(req.name(), req.vendor(), req.region(), req.cdnDomain(), req.encryption(),
                req.versioning() != null && req.versioning(), req.crr() != null && req.crr(),
                req.lifecycleDays() != null ? req.lifecycleDays() : 30,
                req.acl() != null ? req.acl() : "private",
                req.cors() != null ? req.cors() : List.of(),
                req.note(), 0L, 0L);
        registry.put(req.name(), b);
        return b;
    }

    @Override
    public BucketDto update(String name, UpdateBucketRequest req) {
        BucketDto cur = get(name);
        BucketDto b = new BucketDto(cur.name(), cur.vendor(), cur.region(),
                req.cdnDomain() != null ? req.cdnDomain() : cur.cdnDomain(),
                req.encryption() != null ? req.encryption() : cur.encryption(),
                req.versioning() != null ? req.versioning() : cur.versioning(),
                req.crr() != null ? req.crr() : cur.crr(),
                req.lifecycleDays() != null ? req.lifecycleDays() : cur.lifecycleDays(),
                req.acl() != null ? req.acl() : cur.acl(),
                req.cors() != null ? req.cors() : cur.cors(),
                req.note() != null ? req.note() : cur.note(),
                cur.objectCount(), cur.storageBytes());
        registry.put(name, b);
        return b;
    }

    @Override
    public void delete(String name) {
        if (registry.remove(name) == null) {
            throw new UfsException(UfsErrorCode.NOT_FOUND, Map.of("bucket", name));
        }
    }
}
