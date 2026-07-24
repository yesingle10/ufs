package com.ufs.core.adapter.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.config.properties.CosProperties;
import com.ufs.core.spi.InitUploadResult;
import com.ufs.core.spi.ObjectMeta;
import com.ufs.core.spi.PresignResult;
import com.ufs.core.spi.StorageProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 腾讯云 COS 适配器。
 * 使用 COS Java SDK 实现预签名直传/分片上传/对象管理等全部 SPI 方法。
 * 仅在 classpath 存在 COS SDK 时注册（{@link ConditionalOnClass}），
 * 避免 dev-local 等不需要云存储的环境引入 SDK。
 *
 * <h3>配置示例</h3>
 * <pre>{@code
 * ufs.cos.secret-id=AKIDxxx
 * ufs.cos.secret-key=xxx
 * ufs.cos.region=ap-guangzhou
 * ufs.cos.default-bucket=my-bucket-1250000000
 * }</pre>
 */
@Component
@ConditionalOnClass(name = "com.qcloud.cos.COSClient")
public class CosStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(CosStorageProvider.class);

    private final CosProperties props;
    private COSClient client;

    public CosStorageProvider(CosProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (isBlank(props.getSecretId()) || isBlank(props.getSecretKey())) {
            log.info("COS credentials not configured; adapter disabled");
            return;
        }
        COSCredentials cred = new BasicCOSCredentials(props.getSecretId(), props.getSecretKey());
        ClientConfig config = new ClientConfig(new Region(props.getRegion()));
        this.client = new COSClient(cred, config);
        log.info("COS adapter initialized: region={}, defaultBucket={}", props.getRegion(), props.getDefaultBucket());
    }

    @PreDestroy
    void destroy() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Override
    public String vendor() {
        return "cos";
    }

    // ---- 分片上传 ----

    @Override
    public InitUploadResult initUpload(InitUploadContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        try {
            InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(bucket, ctx.objectKey());
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(ctx.contentType());
            meta.setContentLength(ctx.fileSize());
            req.setObjectMetadata(meta);
            InitiateMultipartUploadResult result = client.initiateMultipartUpload(req);
            return new InitUploadResult(result.getUploadId(), partSizeOrDefault(ctx.partSize()), ctx.totalParts());
        } catch (CosClientException e) {
            throw vendorError("initUpload", e);
        }
    }

    // ---- 预签名 ----

    @Override
    public PresignResult presignUpload(PresignContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long expires = expiresAt();
        try {
            Date expiration = new Date(expires);
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, ctx.objectKey(), HttpMethodName.PUT);
            req.setExpiration(expiration);
            if (ctx.contentType() != null && !ctx.contentType().isEmpty()) {
                req.setContentType(ctx.contentType());
            }
            URL url = client.generatePresignedUrl(req);
            return new PresignResult(url.toString(), "PUT",
                    contentDispositionHeader(ctx.fileName()), expires, null, ctx.objectKey(), ctx.bucket());
        } catch (CosClientException e) {
            throw vendorError("presignUpload", e);
        }
    }

    @Override
    public PresignResult presignPartUpload(InitUploadContext ctx, String uploadId, int partNumber) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long expires = expiresAt();
        try {
            Date expiration = new Date(expires);
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, ctx.objectKey(), HttpMethodName.PUT);
            req.setExpiration(expiration);
            req.addRequestParameter("uploadId", uploadId);
            req.addRequestParameter("partNumber", String.valueOf(partNumber));
            URL url = client.generatePresignedUrl(req);
            return new PresignResult(url.toString(), "PUT",
                    Map.of(), expires, uploadId, ctx.objectKey(), ctx.bucket());
        } catch (CosClientException e) {
            throw vendorError("presignPartUpload", e);
        }
    }

    @Override
    public PresignResult presignDownload(PresignContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long expires = expiresAt();
        try {
            Date expiration = new Date(expires);
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, ctx.objectKey(), HttpMethodName.GET);
            req.setExpiration(expiration);
            URL url = client.generatePresignedUrl(req);
            return new PresignResult(url.toString(), "GET",
                    Map.of(), expires, null, ctx.objectKey(), ctx.bucket());
        } catch (CosClientException e) {
            throw vendorError("presignDownload", e);
        }
    }

    // ---- 完成上传 ----

    @Override
    public ObjectMeta completeUpload(CompleteContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        try {
            CompleteMultipartUploadRequest req = new CompleteMultipartUploadRequest(
                    bucket, ctx.objectKey(), ctx.uploadId(),
                    ctx.parts().stream()
                            .map(p -> new com.qcloud.cos.model.PartETag(p.partNumber(), p.etag()))
                            .collect(Collectors.toList()));
            CompleteMultipartUploadResult result = client.completeMultipartUpload(req);
            long now = System.currentTimeMillis();
            return new ObjectMeta(
                    ctx.objectKey(), // fileId — COS 侧用 objectKey 定位
                    ctx.tenantId(), ctx.bucket(), ctx.objectKey(),
                    ctx.objectKey(), ctx.fileSize(), "", ctx.md5(),
                    null, null, null, result.getLocation(),
                    1, now, now);
        } catch (CosClientException e) {
            throw vendorError("completeUpload", e);
        }
    }

    // ---- 对象管理 ----

    @Override
    public List<ObjectMeta> list(ListContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        try {
            ListObjectsRequest req = new ListObjectsRequest();
            req.setBucketName(bucket);
            if (ctx.prefix() != null && !ctx.prefix().isEmpty()) {
                req.setPrefix(ctx.prefix());
            }
            if (ctx.cursor() != null && !ctx.cursor().isEmpty()) {
                req.setMarker(ctx.cursor());
            }
            req.setMaxKeys(ctx.limit());
            ObjectListing result = client.listObjects(req);
            long now = System.currentTimeMillis();
            return result.getObjectSummaries().stream()
                    .map(s -> new ObjectMeta(
                            s.getKey(), ctx.tenantId(), ctx.bucket(), s.getKey(),
                            s.getKey(), s.getSize(), "", s.getETag(),
                            null, null, null, "",
                            1, s.getLastModified().getTime(), now))
                    .collect(Collectors.toList());
        } catch (CosClientException e) {
            throw vendorError("list", e);
        }
    }

    @Override
    public ObjectMeta head(HeadContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        try {
            ObjectMetadata meta = client.getObjectMetadata(bucket, ctx.objectKey());
            long now = System.currentTimeMillis();
            return new ObjectMeta(
                    ctx.objectKey(), ctx.tenantId(), ctx.bucket(), ctx.objectKey(),
                    ctx.objectKey(), meta.getContentLength(), meta.getContentType(), meta.getETag(),
                    null, null, null, "",
                    1, meta.getLastModified().getTime(), now);
        } catch (CosClientException e) {
            throw new UfsException(UfsErrorCode.NOT_FOUND,
                    Map.of("objectKey", ctx.objectKey(), "vendor", vendor()));
        }
    }

    @Override
    public void copy(CopyContext ctx) {
        requireClient();
        String srcBucket = bucketOf(ctx.srcBucket());
        String destBucket = bucketOf(ctx.destBucket());
        try {
            CopyObjectRequest req = new CopyObjectRequest(
                    srcBucket, ctx.srcKey(), destBucket, ctx.destKey());
            client.copyObject(req);
        } catch (CosClientException e) {
            throw vendorError("copy", e);
        }
    }

    @Override
    public void move(MoveContext ctx) {
        copy(new CopyContext(ctx.tenantId(), ctx.srcBucket(), ctx.srcKey(), ctx.destBucket(), ctx.destKey()));
        delete(new DeleteContext(ctx.tenantId(), ctx.srcBucket(), ctx.srcKey()));
    }

    @Override
    public void delete(DeleteContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        try {
            client.deleteObject(bucket, ctx.objectKey());
        } catch (CosClientException e) {
            throw vendorError("delete", e);
        }
    }

    // ---- 内部工具 ----

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String bucketOf(String requestBucket) {
        return requestBucket != null && !requestBucket.isEmpty() ? requestBucket : props.getDefaultBucket();
    }

    private long expiresAt() {
        return System.currentTimeMillis() + props.getPresignExpireSeconds() * 1000;
    }

    private int partSizeOrDefault(int partSize) {
        return partSize > 0 ? partSize : 5 * 1024 * 1024; // 默认 5 MB
    }

    private void requireClient() {
        if (client == null) {
            throw new UfsException(UfsErrorCode.VENDOR_ERROR,
                    Map.of("vendor", vendor(), "reason", "COS credentials not configured"));
        }
    }

    private UfsException vendorError(String operation, CosClientException cause) {
        log.error("COS {} failed: {}", operation, cause.getMessage(), cause);
        return new UfsException(UfsErrorCode.VENDOR_ERROR,
                Map.of("vendor", vendor(), "operation", operation, "reason", cause.getMessage()));
    }

    private static Map<String, String> contentDispositionHeader(String fileName) {
        if (fileName == null || fileName.isEmpty()) return Map.of();
        return Map.of("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
    }
}
