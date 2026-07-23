package com.ufs.core.spi;

import java.util.List;
import java.util.Map;

/**
 * 统一存储接口（SPI）。所有云厂商适配器必须实现，屏蔽 OSS/Kodo/TOS 差异（对齐 Java 规范 §6、PRD §9）。
 * 方法签名以 UFS 领域对象为准，不出现厂商私有类型。
 */
public interface StorageProvider {

    /** 厂商标识：oss / kodo / tos / local。 */
    String vendor();

    /** 初始化分片上传，返回 uploadId。 */
    InitUploadResult initUpload(InitUploadContext ctx);

    /** 单文件（简单）上传预签名 PUT URL。 */
    PresignResult presignUpload(PresignContext ctx);

    /** 分片上传：指定 partNumber 的预签名 PUT URL（需传入 initUpload 返回的 uploadId）。 */
    PresignResult presignPartUpload(InitUploadContext ctx, String uploadId, int partNumber);

    /** 下载/直链预签名 GET URL。 */
    PresignResult presignDownload(PresignContext ctx);

    /** 完成分片上传，返回最终对象元数据。 */
    ObjectMeta completeUpload(CompleteContext ctx);

    /** 列举对象（游标分页）。 */
    List<ObjectMeta> list(ListContext ctx);

    /** 获取对象元数据（不存在抛 UFS.4040）。 */
    ObjectMeta head(HeadContext ctx);

    void copy(CopyContext ctx);

    void move(MoveContext ctx);

    void delete(DeleteContext ctx);

    // ---- 上下文（不可变 record） ----

    record PresignContext(String tenantId, String bucket, String objectKey,
                          String fileName, String contentType, long fileSize) {
    }

    record InitUploadContext(String tenantId, String bucket, String objectKey,
                             String fileName, String contentType, long fileSize,
                             int partSize, int totalParts) {
    }

    record PartETag(int partNumber, String etag) {
    }

    record CompleteContext(String tenantId, String bucket, String objectKey, String uploadId,
                           List<PartETag> parts, long fileSize, String md5) {
    }

    record ListContext(String tenantId, String bucket, String prefix, String cursor, int limit) {
    }

    record HeadContext(String tenantId, String bucket, String objectKey) {
    }

    record CopyContext(String tenantId, String srcBucket, String srcKey, String destBucket, String destKey) {
    }

    record MoveContext(String tenantId, String srcBucket, String srcKey, String destBucket, String destKey) {
    }

    record DeleteContext(String tenantId, String bucket, String objectKey) {
    }

    /** 供 list 返回的分片占位（adapter 不持有真实字节时用）。 */
    static ObjectMeta placeholder(InitUploadContext ctx, String fileId) {
        long now = System.currentTimeMillis();
        return new ObjectMeta(fileId, ctx.tenantId(), ctx.bucket(), ctx.objectKey(), ctx.fileName(),
                ctx.fileSize(), ctx.contentType(), "", null, null, null, "", 1, now, now);
    }

    static ObjectMeta placeholder(PresignContext ctx, String fileId) {
        long now = System.currentTimeMillis();
        return new ObjectMeta(fileId, ctx.tenantId(), ctx.bucket(), ctx.objectKey(), ctx.fileName(),
                ctx.fileSize(), ctx.contentType(), "", null, null, null, "", 1, now, now);
    }
}
