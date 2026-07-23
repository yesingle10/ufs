package com.ufs.core.service.impl;

import com.ufs.core.common.PageResult;
import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.common.util.IdGenerator;
import com.ufs.core.model.dto.object.CompleteUploadRequest;
import com.ufs.core.model.dto.object.CopyMoveRequest;
import com.ufs.core.model.dto.object.ListObjectsRequest;
import com.ufs.core.model.dto.object.ObjectDto;
import com.ufs.core.model.dto.object.PresignUploadRequest;
import com.ufs.core.model.dto.object.PresignUploadResponse;
import com.ufs.core.repository.entity.FileMeta;
import com.ufs.core.repository.mapper.FileMetaMapper;
import com.ufs.core.service.BucketService;
import com.ufs.core.service.ObjectService;
import com.ufs.core.spi.InitUploadResult;
import com.ufs.core.spi.ObjectMeta;
import com.ufs.core.spi.PresignResult;
import com.ufs.core.spi.StorageProvider;
import com.ufs.core.spi.StorageProviderFactory;
import com.ufs.core.tenant.TenantContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对象服务：预签名直传 / 分片完成 / 列举（游标）/ HEAD / 删除 / 复制 / 移动。
 * 路由：bucket → vendor → StorageProvider（factory）；租户取自 TenantContextHolder。
 * 元数据持久化到 file_meta；厂商侧字节不在 UFS 落盘（UFS 只发预签名 URL）。
 */
@Service
public class ObjectServiceImpl implements ObjectService {

    private static final long PRESIGN_TTL_MS = 3600_000L;

    private final StorageProviderFactory factory;
    private final BucketService bucketService;
    private final FileMetaMapper fileMetaMapper;

    public ObjectServiceImpl(StorageProviderFactory factory, BucketService bucketService, FileMetaMapper fileMetaMapper) {
        this.factory = factory;
        this.bucketService = bucketService;
        this.fileMetaMapper = fileMetaMapper;
    }

    @Override
    public PresignUploadResponse presignUpload(PresignUploadRequest req) {
        String tenantId = TenantContextHolder.getTenantId();
        String vendor = bucketService.get(req.bucket()).vendor();
        StorageProvider p = factory.get(vendor);
        String fileId = IdGenerator.newId("f_");
        String objectKey = req.objectKey();
        long expiresAt = System.currentTimeMillis() + PRESIGN_TTL_MS;

        if (req.multipart()) {
            int partSize = req.partSize() != null ? req.partSize() : 8 * 1024 * 1024;
            int totalParts = (int) Math.ceil((double) req.fileSize() / partSize);
            StorageProvider.InitUploadContext iuc = new StorageProvider.InitUploadContext(
                    tenantId, req.bucket(), objectKey, req.fileName(), req.contentType(), req.fileSize(), partSize, totalParts);
            InitUploadResult init = p.initUpload(iuc);
            Map<Integer, String> partUrls = new LinkedHashMap<>();
            for (int i = 1; i <= totalParts; i++) {
                PresignResult pr = p.presignPartUpload(iuc, init.uploadId(), i);
                partUrls.put(i, pr.url());
            }
            return new PresignUploadResponse(fileId, init.uploadId(), objectKey, req.bucket(), "PUT", null, partUrls, expiresAt);
        } else {
            PresignResult pr = p.presignUpload(
                    new StorageProvider.PresignContext(tenantId, req.bucket(), objectKey, req.fileName(), req.contentType(), req.fileSize()));
            return new PresignUploadResponse(fileId, null, objectKey, req.bucket(), pr.method(), pr.url(), null, pr.expiresAt());
        }
    }

    @Override
    public ObjectDto completeUpload(CompleteUploadRequest req) {
        String tenantId = TenantContextHolder.getTenantId();
        String vendor = bucketService.get(req.bucket()).vendor();
        StorageProvider p = factory.get(vendor);
        List<StorageProvider.PartETag> parts = req.parts() == null ? List.of()
                : req.parts().stream().map(x -> new StorageProvider.PartETag(x.partNumber(), x.etag())).toList();
        ObjectMeta meta = p.completeUpload(
                new StorageProvider.CompleteContext(tenantId, req.bucket(), req.objectKey(), req.uploadId(), parts, req.fileSize(), req.md5()));

        long now = System.currentTimeMillis();
        FileMeta fm = new FileMeta(meta.fileId(), tenantId, req.bizType(), vendor, req.bucket(), req.objectKey(),
                fileNameOf(req.objectKey()), req.fileSize(), meta.contentType(), req.md5(),
                null, null, null, meta.url(), 1, null, null, 0, now, now, null);
        fileMetaMapper.insert(fm);
        return ObjectDto.from(fm);
    }

    @Override
    public PageResult<ObjectDto> list(ListObjectsRequest req) {
        String tenantId = TenantContextHolder.getTenantId();
        long cursor = safeCursor(req.cursor());
        int limit = req.limit();
        List<FileMeta> rows = fileMetaMapper.listByCursor(tenantId, req.bizType(), req.provider(), req.status(), cursor, limit + 1);
        boolean hasMore = rows.size() > limit;
        if (hasMore) {
            rows = rows.subList(0, limit);
        }
        List<ObjectDto> dtos = rows.stream().map(ObjectDto::from).toList();
        String next = hasMore && !rows.isEmpty() ? String.valueOf(rows.get(rows.size() - 1).createdAt()) : null;
        return PageResult.of(dtos, next, hasMore, limit);
    }

    @Override
    public ObjectDto head(String objectKey) {
        String tenantId = TenantContextHolder.getTenantId();
        FileMeta m = fileMetaMapper.selectByKey(tenantId, objectKey);
        if (m == null) {
            throw new UfsException(UfsErrorCode.NOT_FOUND, Map.of("objectKey", objectKey));
        }
        return ObjectDto.from(m);
    }

    @Override
    public void delete(String objectKey) {
        String tenantId = TenantContextHolder.getTenantId();
        FileMeta m = fileMetaMapper.selectByKey(tenantId, objectKey);
        if (m == null) {
            throw new UfsException(UfsErrorCode.NOT_FOUND, Map.of("objectKey", objectKey));
        }
        StorageProvider p = factory.get(m.provider());
        p.delete(new StorageProvider.DeleteContext(tenantId, m.bucket(), objectKey));
        fileMetaMapper.softDelete(tenantId, m.fileId(), System.currentTimeMillis());
    }

    @Override
    public void copy(CopyMoveRequest req) {
        String tenantId = TenantContextHolder.getTenantId();
        StorageProvider p = factory.get(bucketService.get(req.bucket()).vendor());
        p.copy(new StorageProvider.CopyContext(tenantId, req.bucket(), req.srcObjectKey(), req.bucket(), req.destObjectKey()));
    }

    @Override
    public void move(CopyMoveRequest req) {
        String tenantId = TenantContextHolder.getTenantId();
        StorageProvider p = factory.get(bucketService.get(req.bucket()).vendor());
        p.move(new StorageProvider.MoveContext(tenantId, req.bucket(), req.srcObjectKey(), req.bucket(), req.destObjectKey()));
    }

    private static long safeCursor(String c) {
        if (c == null) {
            return Long.MAX_VALUE;
        }
        try {
            return Long.parseLong(c);
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    private static String fileNameOf(String objectKey) {
        int i = objectKey.lastIndexOf('/');
        return i >= 0 ? objectKey.substring(i + 1) : objectKey;
    }
}
