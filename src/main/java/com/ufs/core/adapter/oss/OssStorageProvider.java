package com.ufs.core.adapter.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.ServiceException;
import com.aliyun.oss.model.*;
import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.config.properties.OssProperties;
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
 * 阿里云 OSS 适配器。
 * 仅在 classpath 存在 OSS SDK 时注册。
 */
@Component
@ConditionalOnClass(name = "com.aliyun.oss.OSS")
public class OssStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(OssStorageProvider.class);

    private final OssProperties props;
    private OSS client;

    public OssStorageProvider(OssProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (isBlank(props.getAccessKeyId()) || isBlank(props.getAccessKeySecret())) {
            log.info("OSS credentials not configured; adapter disabled");
            return;
        }
        this.client = new OSSClientBuilder().build(props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
        log.info("OSS adapter initialized: endpoint={}, defaultBucket={}", props.getEndpoint(), props.getDefaultBucket());
    }

    @PreDestroy
    void destroy() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Override
    public String vendor() {
        return "oss";
    }

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
        } catch (ServiceException e) {
            throw vendorError("initUpload", e);
        }
    }

    @Override
    public PresignResult presignUpload(PresignContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long expires = expiresAt();
        try {
            Date expiration = new Date(expires);
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, ctx.objectKey(), com.aliyun.oss.HttpMethod.PUT);
            req.setExpiration(expiration);
            if (ctx.contentType() != null && !ctx.contentType().isEmpty()) {
                req.setContentType(ctx.contentType());
            }
            if (ctx.fileName() != null && !ctx.fileName().isEmpty()) {
                req.addHeader("Content-Disposition", "attachment; filename=\"" + ctx.fileName() + "\"");
            }
            URL url = client.generatePresignedUrl(req);
            return new PresignResult(url.toString(), "PUT",
                    contentDispositionHeader(ctx.fileName()), expires, null, ctx.objectKey(), ctx.bucket());
        } catch (ServiceException e) {
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
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, ctx.objectKey(), com.aliyun.oss.HttpMethod.PUT);
            req.setExpiration(expiration);
            req.addQueryParameter("uploadId", uploadId);
            req.addQueryParameter("partNumber", String.valueOf(partNumber));
            URL url = client.generatePresignedUrl(req);
            return new PresignResult(url.toString(), "PUT", Map.of(), expires, uploadId, ctx.objectKey(), ctx.bucket());
        } catch (ServiceException e) {
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
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, ctx.objectKey(), com.aliyun.oss.HttpMethod.GET);
            req.setExpiration(expiration);
            URL url = client.generatePresignedUrl(req);
            return new PresignResult(url.toString(), "GET", Map.of(), expires, null, ctx.objectKey(), ctx.bucket());
        } catch (ServiceException e) {
            throw vendorError("presignDownload", e);
        }
    }

    @Override
    public ObjectMeta completeUpload(CompleteContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        try {
            CompleteMultipartUploadRequest req = new CompleteMultipartUploadRequest(
                    bucket, ctx.objectKey(), ctx.uploadId(),
                    ctx.parts().stream()
                            .map(p -> new com.aliyun.oss.model.PartETag(p.partNumber(), p.etag()))
                            .collect(Collectors.toList()));
            CompleteMultipartUploadResult result = client.completeMultipartUpload(req);
            long now = System.currentTimeMillis();
            return new ObjectMeta(
                    ctx.objectKey(), ctx.tenantId(), ctx.bucket(), ctx.objectKey(),
                    ctx.objectKey(), ctx.fileSize(), "", ctx.md5(),
                    null, null, null, result.getLocation(),
                    1, now, now);
        } catch (ServiceException e) {
            throw vendorError("completeUpload", e);
        }
    }

    @Override
    public List<ObjectMeta> list(ListContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        try {
            ListObjectsRequest req = new ListObjectsRequest(bucket);
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
        } catch (ServiceException e) {
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
        } catch (OSSException e) {
            if (OSSErrorCode.NO_SUCH_KEY.equals(e.getErrorCode())) {
                throw new UfsException(UfsErrorCode.NOT_FOUND,
                        Map.of("objectKey", ctx.objectKey(), "vendor", vendor()));
            }
            throw vendorError("head", e);
        } catch (ServiceException e) {
            throw vendorError("head", e);
        }
    }

    @Override
    public void copy(CopyContext ctx) {
        requireClient();
        String srcBucket = bucketOf(ctx.srcBucket());
        String destBucket = bucketOf(ctx.destBucket());
        try {
            client.copyObject(srcBucket, ctx.srcKey(), destBucket, ctx.destKey());
        } catch (ServiceException e) {
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
        } catch (ServiceException e) {
            throw vendorError("delete", e);
        }
    }

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
        return partSize > 0 ? partSize : 5 * 1024 * 1024;
    }

    private void requireClient() {
        if (client == null) {
            throw new UfsException(UfsErrorCode.VENDOR_ERROR,
                    Map.of("vendor", vendor(), "reason", "OSS credentials not configured"));
        }
    }

    private UfsException vendorError(String operation, ServiceException cause) {
        log.error("OSS {} failed: {}", operation, cause.getMessage(), cause);
        return new UfsException(UfsErrorCode.VENDOR_ERROR,
                Map.of("vendor", vendor(), "operation", operation, "reason", cause.getMessage()));
    }

    private static Map<String, String> contentDispositionHeader(String fileName) {
        if (fileName == null || fileName.isEmpty()) return Map.of();
        return Map.of("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
    }
}
