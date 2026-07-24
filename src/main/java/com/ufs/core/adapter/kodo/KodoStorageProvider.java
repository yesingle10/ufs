package com.ufs.core.adapter.kodo;

import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;
import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.common.util.IdGenerator;
import com.ufs.core.config.properties.KodoProperties;
import com.ufs.core.spi.InitUploadResult;
import com.ufs.core.spi.ObjectMeta;
import com.ufs.core.spi.PresignResult;
import com.ufs.core.spi.StorageProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 七牛云 Kodo 适配器（SDK v7.15+）。
 * Kodo 的断点续传由客户端 SDK 自动管理，服务端侧无需显式的 init/complete 分片流程。
 * adapter 通过 uploadToken 实现预签名，通过 BucketManager 实现对象管理。
 */
@Component
@ConditionalOnClass(name = "com.qiniu.util.Auth")
public class KodoStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(KodoStorageProvider.class);

    private final KodoProperties props;
    private Auth auth;
    private BucketManager bucketManager;

    public KodoStorageProvider(KodoProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (isBlank(props.getAccessKey()) || isBlank(props.getSecretKey())) {
            log.info("Kodo credentials not configured; adapter disabled");
            return;
        }
        this.auth = Auth.create(props.getAccessKey(), props.getSecretKey());
        this.bucketManager = new BucketManager(auth, new Configuration());
        log.info("Kodo adapter initialized: defaultBucket={}", props.getDefaultBucket());
    }

    @Override
    public String vendor() {
        return "kodo";
    }

    // Kodo 服务端无显式 init multipart；返回一个 token 作为 uploadId 即可
    @Override
    public InitUploadResult initUpload(InitUploadContext ctx) {
        requireClient();
        String uploadId = "kodo_" + IdGenerator.newId("u_");
        return new InitUploadResult(uploadId, partSizeOrDefault(ctx.partSize()), ctx.totalParts());
    }

    @Override
    public PresignResult presignUpload(PresignContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long expires = expiresAt();
        String token = auth.uploadToken(bucket, ctx.objectKey(), props.getPresignExpireSeconds(), null, true);
        String url = uploadUrl();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "UpToken " + token);
        if (ctx.contentType() != null && !ctx.contentType().isEmpty()) {
            headers.put("Content-Type", ctx.contentType());
        }
        return new PresignResult(url, "PUT", headers, expires, null, ctx.objectKey(), ctx.bucket());
    }

    @Override
    public PresignResult presignPartUpload(InitUploadContext ctx, String uploadId, int partNumber) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long expires = expiresAt();
        // Kodo 分片通过 mkblk/bput/mkfile 的 HTTP API 实现，这里生成带 token 的直传 URL
        String token = auth.uploadToken(bucket, ctx.objectKey(), props.getPresignExpireSeconds(), null, true);
        String url = uploadUrl() + "/mkblk/" + (partNumber == 1 ? partSizeOrDefault(ctx.partSize()) : ctx.partSize());
        return new PresignResult(url, "PUT",
                Map.of("Authorization", "UpToken " + token), expires, uploadId, ctx.objectKey(), ctx.bucket());
    }

    @Override
    public PresignResult presignDownload(PresignContext ctx) {
        requireClient();
        long expires = expiresAt();
        String baseUrl = downloadBaseUrl() + "/" + ctx.objectKey();
        String url;
        try {
            // 私有空间下载签名
            url = auth.privateDownloadUrl(baseUrl, props.getPresignExpireSeconds());
            if (ctx.fileName() != null && !ctx.fileName().isEmpty()) {
                url += "&attname=" + ctx.fileName();
            }
        } catch (Exception e) {
            url = baseUrl;
        }
        return new PresignResult(url, "GET", Map.of(), expires, null, ctx.objectKey(), ctx.bucket());
    }

    @Override
    public ObjectMeta completeUpload(CompleteContext ctx) {
        requireClient();
        // Kodo SDK 的断点续传在客户端完成；这里返回元数据占位
        long now = System.currentTimeMillis();
        return new ObjectMeta(
                ctx.objectKey(), ctx.tenantId(), ctx.bucket(), ctx.objectKey(),
                ctx.objectKey(), ctx.fileSize(), "", "",
                null, null, null, downloadBaseUrl() + "/" + ctx.objectKey(),
                1, now, now);
    }

    @Override
    public List<ObjectMeta> list(ListContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long now = System.currentTimeMillis();
        try {
            FileListing listing = bucketManager.listFiles(bucket,
                    ctx.prefix() != null ? ctx.prefix() : "",
                    ctx.cursor() != null ? ctx.cursor() : "",
                    ctx.limit(), "/");
            if (listing.items == null) return List.of();
            return Arrays.stream(listing.items)
                    .map(fi -> new ObjectMeta(
                            fi.key, ctx.tenantId(), ctx.bucket(), fi.key,
                            fi.key, fi.fsize, fi.mimeType, fi.hash,
                            null, null, null, downloadBaseUrl() + "/" + fi.key,
                            1, fi.putTime / 10000, now))
                    .toList();
        } catch (QiniuException e) {
            throw vendorError("list", e);
        }
    }

    @Override
    public ObjectMeta head(HeadContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long now = System.currentTimeMillis();
        try {
            FileInfo fi = bucketManager.stat(bucket, ctx.objectKey());
            return new ObjectMeta(
                    ctx.objectKey(), ctx.tenantId(), ctx.bucket(), ctx.objectKey(),
                    ctx.objectKey(), fi.fsize, fi.mimeType, fi.hash,
                    null, null, null, downloadBaseUrl() + "/" + ctx.objectKey(),
                    1, fi.putTime / 10000, now);
        } catch (QiniuException e) {
            if (e.response != null && e.response.statusCode == 612) {
                throw new UfsException(UfsErrorCode.NOT_FOUND,
                        Map.of("objectKey", ctx.objectKey(), "vendor", vendor()));
            }
            throw vendorError("head", e);
        }
    }

    @Override
    public void copy(CopyContext ctx) {
        requireClient();
        String srcBucket = bucketOf(ctx.srcBucket());
        String destBucket = bucketOf(ctx.destBucket());
        try {
            bucketManager.copy(srcBucket, ctx.srcKey(), destBucket, ctx.destKey(), true);
        } catch (QiniuException e) {
            throw vendorError("copy", e);
        }
    }

    @Override
    public void move(MoveContext ctx) {
        requireClient();
        String srcBucket = bucketOf(ctx.srcBucket());
        String destBucket = bucketOf(ctx.destBucket());
        try {
            bucketManager.move(srcBucket, ctx.srcKey(), destBucket, ctx.destKey(), true);
        } catch (QiniuException e) {
            throw vendorError("move", e);
        }
    }

    @Override
    public void delete(DeleteContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        try {
            bucketManager.delete(bucket, ctx.objectKey());
        } catch (QiniuException e) {
            throw vendorError("delete", e);
        }
    }

    private String bucketOf(String requestBucket) {
        return requestBucket != null && !requestBucket.isEmpty() ? requestBucket : props.getDefaultBucket();
    }

    private long expiresAt() {
        return System.currentTimeMillis() + props.getPresignExpireSeconds() * 1000;
    }

    private int partSizeOrDefault(int partSize) {
        return partSize > 0 ? partSize : 4 * 1024 * 1024;
    }

    private String uploadUrl() {
        String region = props.getRegionUrl();
        return region != null ? region : "https://upload.qiniup.com";
    }

    private String downloadBaseUrl() {
        // 七牛为每个空间分配独立下载域名，需在配置中指定
        return "https://" + (props.getDefaultBucket() != null ? props.getDefaultBucket() + ".qiniudns.com" : "cdn.example.com");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void requireClient() {
        if (auth == null) {
            throw new UfsException(UfsErrorCode.VENDOR_ERROR,
                    Map.of("vendor", vendor(), "reason", "Kodo credentials not configured"));
        }
    }

    private UfsException vendorError(String operation, QiniuException cause) {
        String detail = cause != null ? cause.getMessage() : "unknown";
        if (cause != null && cause.response != null) {
            detail = cause.response.toString();
        }
        log.error("Kodo {} failed: {}", operation, detail);
        return new UfsException(UfsErrorCode.VENDOR_ERROR,
                Map.of("vendor", vendor(), "operation", operation, "reason", detail));
    }
}
