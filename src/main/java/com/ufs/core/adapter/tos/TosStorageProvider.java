package com.ufs.core.adapter.tos;

import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import com.volcengine.tos.TosException;
import com.volcengine.tos.TosServerException;
import com.volcengine.tos.auth.StaticCredentials;
import com.volcengine.tos.comm.HttpMethod;
import com.volcengine.tos.model.object.*;
import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.config.properties.TosProperties;
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
import java.util.stream.Collectors;

/**
 * 火山引擎 TOS 适配器（SDK v2.x）。
 * 仅在 classpath 存在 TOS SDK 时注册。
 */
@Component
@ConditionalOnClass(name = "com.volcengine.tos.TOSV2")
public class TosStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(TosStorageProvider.class);

    private final TosProperties props;
    private TOSV2 client;

    public TosStorageProvider(TosProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (isBlank(props.getAccessKeyId()) || isBlank(props.getAccessKeySecret())) {
            log.info("TOS credentials not configured; adapter disabled");
            return;
        }
        StaticCredentials cred = new StaticCredentials(props.getAccessKeyId(), props.getAccessKeySecret());
        this.client = new TOSV2ClientBuilder()
                .build(props.getRegion(), props.getEndpoint(), cred);
        log.info("TOS adapter initialized: endpoint={}, region={}, defaultBucket={}",
                props.getEndpoint(), props.getRegion(), props.getDefaultBucket());
    }

    @Override
    public String vendor() {
        return "tos";
    }

    @Override
    public InitUploadResult initUpload(InitUploadContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        try {
            CreateMultipartUploadInput input = new CreateMultipartUploadInput()
                    .setBucket(bucket)
                    .setKey(ctx.objectKey());
            CreateMultipartUploadOutput output = client.createMultipartUpload(input);
            return new InitUploadResult(output.getUploadID(), partSizeOrDefault(ctx.partSize()), ctx.totalParts());
        } catch (TosException e) {
            throw vendorError("initUpload", e);
        }
    }

    @Override
    public PresignResult presignUpload(PresignContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long expires = expiresAt();
        try {
            Map<String, String> headers = new HashMap<>();
            if (ctx.contentType() != null && !ctx.contentType().isEmpty()) {
                headers.put("Content-Type", ctx.contentType());
            }
            if (ctx.fileName() != null && !ctx.fileName().isEmpty()) {
                headers.put("Content-Disposition", "attachment; filename=\"" + ctx.fileName() + "\"");
            }
            PreSignedURLInput input = new PreSignedURLInput()
                    .setBucket(bucket)
                    .setKey(ctx.objectKey())
                    .setHttpMethod(HttpMethod.PUT)
                    .setExpires(props.getPresignExpireSeconds());
            if (!headers.isEmpty()) {
                input.setHeader(headers);
            }
            PreSignedURLOutput output = client.preSignedURL(input);
            return new PresignResult(output.getSignedUrl(), "PUT",
                    contentDispositionHeader(ctx.fileName()), expires, null, ctx.objectKey(), ctx.bucket());
        } catch (TosException e) {
            throw vendorError("presignUpload", e);
        }
    }

    @Override
    public PresignResult presignPartUpload(InitUploadContext ctx, String uploadId, int partNumber) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long expires = expiresAt();
        try {
            Map<String, String> query = new HashMap<>();
            query.put("uploadId", uploadId);
            query.put("partNumber", String.valueOf(partNumber));
            PreSignedURLInput input = new PreSignedURLInput()
                    .setBucket(bucket)
                    .setKey(ctx.objectKey())
                    .setHttpMethod(HttpMethod.PUT)
                    .setExpires(props.getPresignExpireSeconds())
                    .setQuery(query);
            PreSignedURLOutput output = client.preSignedURL(input);
            return new PresignResult(output.getSignedUrl(), "PUT", Map.of(), expires, uploadId, ctx.objectKey(), ctx.bucket());
        } catch (TosException e) {
            throw vendorError("presignPartUpload", e);
        }
    }

    @Override
    public PresignResult presignDownload(PresignContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long expires = expiresAt();
        try {
            Map<String, String> query = new HashMap<>();
            if (ctx.fileName() != null && !ctx.fileName().isEmpty()) {
                query.put("response-content-disposition", "attachment; filename=\"" + ctx.fileName() + "\"");
            }
            PreSignedURLInput input = new PreSignedURLInput()
                    .setBucket(bucket)
                    .setKey(ctx.objectKey())
                    .setHttpMethod(HttpMethod.GET)
                    .setExpires(props.getPresignExpireSeconds());
            if (!query.isEmpty()) {
                input.setQuery(query);
            }
            PreSignedURLOutput output = client.preSignedURL(input);
            return new PresignResult(output.getSignedUrl(), "GET", Map.of(), expires, null, ctx.objectKey(), ctx.bucket());
        } catch (TosException e) {
            throw vendorError("presignDownload", e);
        }
    }

    @Override
    public ObjectMeta completeUpload(CompleteContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long now = System.currentTimeMillis();
        try {
            CompleteMultipartUploadV2Input input = new CompleteMultipartUploadV2Input()
                    .setBucket(bucket)
                    .setKey(ctx.objectKey())
                    .setUploadID(ctx.uploadId())
                    .setUploadedParts(ctx.parts().stream()
                            .map(p -> new UploadedPartV2()
                                    .setPartNumber(p.partNumber())
                                    .setEtag(p.etag()))
                            .collect(Collectors.toList()));
            client.completeMultipartUpload(input);

            return new ObjectMeta(
                    ctx.objectKey(), ctx.tenantId(), ctx.bucket(), ctx.objectKey(),
                    ctx.objectKey(), ctx.fileSize(), "", ctx.md5(),
                    null, null, null, "", 1, now, now);
        } catch (TosException e) {
            throw vendorError("completeUpload", e);
        }
    }

    @Override
    public List<ObjectMeta> list(ListContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long now = System.currentTimeMillis();
        try {
            ListObjectsType2Input input = new ListObjectsType2Input()
                    .setBucket(bucket)
                    .setMaxKeys(ctx.limit());
            if (ctx.prefix() != null && !ctx.prefix().isEmpty()) {
                input.setPrefix(ctx.prefix());
            }
            if (ctx.cursor() != null && !ctx.cursor().isEmpty()) {
                input.setContinuationToken(ctx.cursor());
            }
            ListObjectsType2Output output = client.listObjectsType2(input);
            if (output.getContents() == null) return List.of();
            return output.getContents().stream()
                    .map(s -> new ObjectMeta(
                            s.getKey(), ctx.tenantId(), ctx.bucket(), s.getKey(),
                            s.getKey(), s.getSize(), "", s.getEtag(),
                            null, null, null, "",
                            1, now, now))
                    .collect(Collectors.toList());
        } catch (TosException e) {
            throw vendorError("list", e);
        }
    }

    @Override
    public ObjectMeta head(HeadContext ctx) {
        requireClient();
        String bucket = bucketOf(ctx.bucket());
        long now = System.currentTimeMillis();
        try {
            HeadObjectV2Input input = new HeadObjectV2Input()
                    .setBucket(bucket)
                    .setKey(ctx.objectKey());
            HeadObjectV2Output output = client.headObject(input);
            return new ObjectMeta(
                    ctx.objectKey(), ctx.tenantId(), ctx.bucket(), ctx.objectKey(),
                    ctx.objectKey(), output.getContentLength(), output.getContentType(), output.getEtag(),
                    null, null, null, "",
                    1, now, now);
        } catch (TosServerException e) {
            if (e.getStatusCode() == 404) {
                throw new UfsException(UfsErrorCode.NOT_FOUND,
                        Map.of("objectKey", ctx.objectKey(), "vendor", vendor()));
            }
            throw vendorError("head", e);
        } catch (TosException e) {
            throw vendorError("head", e);
        }
    }

    @Override
    public void copy(CopyContext ctx) {
        requireClient();
        String destBucket = bucketOf(ctx.destBucket());
        try {
            CopyObjectV2Input input = new CopyObjectV2Input()
                    .setBucket(destBucket)
                    .setKey(ctx.destKey())
                    .setSrcBucket(ctx.srcBucket())
                    .setSrcKey(ctx.srcKey());
            client.copyObject(input);
        } catch (TosException e) {
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
            DeleteObjectInput input = new DeleteObjectInput()
                    .setBucket(bucket)
                    .setKey(ctx.objectKey());
            client.deleteObject(input);
        } catch (TosException e) {
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
                    Map.of("vendor", vendor(), "reason", "TOS credentials not configured"));
        }
    }

    private UfsException vendorError(String operation, TosException cause) {
        log.error("TOS {} failed: {}", operation, cause.getMessage(), cause);
        return new UfsException(UfsErrorCode.VENDOR_ERROR,
                Map.of("vendor", vendor(), "operation", operation, "reason", cause.getMessage()));
    }

    private static Map<String, String> contentDispositionHeader(String fileName) {
        if (fileName == null || fileName.isEmpty()) return Map.of();
        return Map.of("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
    }
}
