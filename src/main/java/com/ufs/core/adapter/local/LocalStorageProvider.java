package com.ufs.core.adapter.local;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.common.util.IdGenerator;
import com.ufs.core.spi.InitUploadResult;
import com.ufs.core.spi.ObjectMeta;
import com.ufs.core.spi.PresignResult;
import com.ufs.core.spi.StorageProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地存储适配器（开发/测试用，不依赖真实云厂商，对齐 Java 规范 §12 测试约定）。
 * 字节实际不进 UFS（UFS 只存元数据 + 发预签名 URL），这里用内存 map 模拟“厂商侧对象”。
 */
@Component
public class LocalStorageProvider implements StorageProvider {

    private final Map<String, ObjectMeta> store = new ConcurrentHashMap<>();

    private static String k(String tenantId, String bucket, String key) {
        return tenantId + "/" + bucket + "/" + key;
    }

    @Override
    public String vendor() {
        return "local";
    }

    @Override
    public InitUploadResult initUpload(InitUploadContext ctx) {
        return new InitUploadResult(IdGenerator.newId("u_"), ctx.partSize(), ctx.totalParts());
    }

    @Override
    public PresignResult presignUpload(PresignContext ctx) {
        return new PresignResult(localUrl(ctx.objectKey()), "PUT", Map.of("Content-Type", orEmpty(ctx.contentType())),
                System.currentTimeMillis() + 3600_000, null, ctx.objectKey(), ctx.bucket());
    }

    @Override
    public PresignResult presignPartUpload(InitUploadContext ctx, String uploadId, int partNumber) {
        return new PresignResult(localUrl(ctx.objectKey() + "?part=" + partNumber), "PUT",
                Map.of(), System.currentTimeMillis() + 3600_000, uploadId, ctx.objectKey(), ctx.bucket());
    }

    @Override
    public PresignResult presignDownload(PresignContext ctx) {
        return new PresignResult(localUrl(ctx.objectKey()), "GET", Map.of(),
                System.currentTimeMillis() + 3600_000, null, ctx.objectKey(), ctx.bucket());
    }

    @Override
    public ObjectMeta completeUpload(CompleteContext ctx) {
        ObjectMeta meta = StorageProvider.placeholder(
                new InitUploadContext(ctx.tenantId(), ctx.bucket(), ctx.objectKey(), "", "", ctx.fileSize(), 0, 0),
                IdGenerator.newId("f_"));
        store.put(k(ctx.tenantId(), ctx.bucket(), ctx.objectKey()), meta);
        return meta;
    }

    @Override
    public List<ObjectMeta> list(ListContext ctx) {
        return store.values().stream()
                .filter(m -> m.tenantId().equals(ctx.tenantId()) && m.bucket().equals(ctx.bucket())
                        && (ctx.prefix() == null || m.objectKey().startsWith(ctx.prefix())))
                .limit(ctx.limit())
                .toList();
    }

    @Override
    public ObjectMeta head(HeadContext ctx) {
        ObjectMeta m = store.get(k(ctx.tenantId(), ctx.bucket(), ctx.objectKey()));
        if (m == null) {
            throw new UfsException(UfsErrorCode.NOT_FOUND, Map.of("objectKey", ctx.objectKey()));
        }
        return m;
    }

    @Override
    public void copy(CopyContext ctx) {
        ObjectMeta src = head(new HeadContext(ctx.tenantId(), ctx.srcBucket(), ctx.srcKey()));
        store.put(k(ctx.tenantId(), ctx.destBucket(), ctx.destKey()), src);
    }

    @Override
    public void move(MoveContext ctx) {
        copy(new CopyContext(ctx.tenantId(), ctx.srcBucket(), ctx.srcKey(), ctx.destBucket(), ctx.destKey()));
        delete(new DeleteContext(ctx.tenantId(), ctx.srcBucket(), ctx.srcKey()));
    }

    @Override
    public void delete(DeleteContext ctx) {
        if (store.remove(k(ctx.tenantId(), ctx.bucket(), ctx.objectKey())) == null) {
            throw new UfsException(UfsErrorCode.NOT_FOUND, Map.of("objectKey", ctx.objectKey()));
        }
    }

    private static String localUrl(String key) {
        return "http://localhost:8080/local/object?key=" + key;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
