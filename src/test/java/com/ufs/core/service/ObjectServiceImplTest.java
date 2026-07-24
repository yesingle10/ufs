package com.ufs.core.service;

import com.ufs.core.common.PageResult;
import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.model.dto.bucket.BucketDto;
import com.ufs.core.model.dto.object.*;
import com.ufs.core.repository.entity.FileMeta;
import com.ufs.core.repository.mapper.FileMetaMapper;
import com.ufs.core.service.impl.ObjectServiceImpl;
import com.ufs.core.spi.*;
import com.ufs.core.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ObjectService 单元测试")
@ExtendWith(MockitoExtension.class)
class ObjectServiceImplTest {

    @Mock private StorageProviderFactory factory;
    @Mock private BucketService bucketService;
    @Mock private FileMetaMapper fileMetaMapper;
    @Mock private StorageProvider provider;

    private ObjectServiceImpl service;
    private static final String TENANT = "t_demo";
    private static final long NOW = Instant.now().toEpochMilli();

    private static final BucketDto DEMO_BUCKET = new BucketDto(
            "ufs-demo-avatar", "local", "cn-hangzhou", null,
            null, false, false, 30, "private", List.of(), null, 0, 0
    );

    @BeforeEach
    void setUp() {
        TenantContextHolder.set(new TenantContextHolder.Context(TENANT, "op", "req-01", "app_demo", false));
        service = new ObjectServiceImpl(factory, bucketService, fileMetaMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ── presignUpload ─────────────────────────────────────

    @Nested
    @DisplayName("presignUpload")
    class PresignUpload {

        @Test
        @DisplayName("简单上传: 返回预签名 URL")
        void shouldReturnPresignedUrlForSimpleUpload() {
            PresignUploadRequest req = new PresignUploadRequest(
                    "ufs-demo-avatar", "test/photo.jpg", "photo.jpg",
                    "image/jpeg", 102400L, "avatar", false, null
            );
            when(bucketService.get("ufs-demo-avatar")).thenReturn(DEMO_BUCKET);
            when(factory.get("local")).thenReturn(provider);
            when(provider.presignUpload(any(StorageProvider.PresignContext.class)))
                    .thenReturn(new PresignResult(
                            "https://local.test/upload?token=xyz", "PUT",
                            Map.of("x-custom", "val"), NOW + 3600_000L,
                            null, "test/photo.jpg", "ufs-demo-avatar"
                    ));

            PresignUploadResponse resp = service.presignUpload(req);

            assertThat(resp.url()).isEqualTo("https://local.test/upload?token=xyz");
            assertThat(resp.method()).isEqualTo("PUT");
            assertThat(resp.expiresAt()).isGreaterThan(NOW);
            assertThat(resp.fileId()).isNotNull();
        }

        @Test
        @DisplayName("分片上传: 返回 uploadId 和分片 URL")
        void shouldReturnPartUrlsForMultipartUpload() {
            PresignUploadRequest req = new PresignUploadRequest(
                    "ufs-demo-avatar", "big/file.dat", "file.dat",
                    "application/octet-stream", 50_000_000L, "avatar", true, 10_485_760
            );
            when(bucketService.get("ufs-demo-avatar")).thenReturn(DEMO_BUCKET);
            when(factory.get("local")).thenReturn(provider);
            when(provider.initUpload(any(StorageProvider.InitUploadContext.class)))
                    .thenReturn(new InitUploadResult("upload-abc123", 10_485_760, 5));
            when(provider.presignPartUpload(any(StorageProvider.InitUploadContext.class), eq("upload-abc123"), any(int.class)))
                    .thenReturn(new PresignResult(
                            "https://local.test/part?part=1", "PUT",
                            Map.of(), NOW + 3600_000L,
                            "upload-abc123", "big/file.dat", "ufs-demo-avatar"
                    ));

            PresignUploadResponse resp = service.presignUpload(req);

            assertThat(resp.uploadId()).isEqualTo("upload-abc123");
            assertThat(resp.partUrls()).hasSize(5);
            assertThat(resp.partUrls().get(1)).contains("part=1");
            assertThat(resp.fileId()).isNotNull();
        }
    }

    // ── completeUpload ────────────────────────────────────

    @Test
    @DisplayName("completeUpload: 完成上传并写入 FileMeta")
    void shouldCompleteUploadAndPersistMeta() {
        CompleteUploadRequest req = new CompleteUploadRequest(
                "upload-abc123", "f_001", "test/photo.jpg",
                "ufs-demo-avatar", "avatar", 102400L, "d41d8cd98f00b204e9800998ecf8427e",
                List.of()
        );
        when(bucketService.get("ufs-demo-avatar")).thenReturn(DEMO_BUCKET);
        when(factory.get("local")).thenReturn(provider);
        when(provider.completeUpload(any(StorageProvider.CompleteContext.class)))
                .thenReturn(new ObjectMeta(
                        "f_001", TENANT, "ufs-demo-avatar", "test/photo.jpg",
                        "photo.jpg", 102400L, "image/jpeg",
                        "d41d8cd98f00b204e9800998ecf8427e",
                        null, null, null,
                        "https://cdn.example.com/test/photo.jpg",
                        1, NOW, NOW
                ));

        ObjectDto result = service.completeUpload(req);

        assertThat(result.fileId()).isEqualTo("f_001");
        assertThat(result.objectKey()).isEqualTo("test/photo.jpg");
        verify(fileMetaMapper).insert(any(FileMeta.class));
    }

    // ── list ──────────────────────────────────────────────

    @Test
    @DisplayName("list: 游标分页返回对象列表")
    void shouldListObjectsWithCursorPagination() {
        long now = Instant.now().toEpochMilli();
        FileMeta fm1 = fileMeta("f_001", "test/a.jpg", "avatar", 1, now);
        FileMeta fm2 = fileMeta("f_002", "test/b.jpg", "avatar", 1, now - 1000);
        ListObjectsRequest req = new ListObjectsRequest("avatar", null, null, null, 2);

        when(fileMetaMapper.listByCursor(anyString(), anyString(), any(), any(), anyLong(), anyInt()))
                .thenReturn(List.of(fm1, fm2));

        PageResult<ObjectDto> page = service.list(req);

        assertThat(page.getList()).hasSize(2);
        assertThat(page.getList().get(0).objectKey()).isEqualTo("test/a.jpg");
    }

    // ── head ──────────────────────────────────────────────

    @Test
    @DisplayName("head: 返回对象元数据")
    void shouldReturnObjectMetadata() {
        long now = Instant.now().toEpochMilli();
        FileMeta fm = fileMeta("f_001", "test/photo.jpg", "avatar", 1, now);
        when(fileMetaMapper.selectByKey(TENANT, "test/photo.jpg")).thenReturn(fm);

        ObjectDto obj = service.head("test/photo.jpg");

        assertThat(obj.fileId()).isEqualTo("f_001");
        assertThat(obj.objectKey()).isEqualTo("test/photo.jpg");
        assertThat(obj.bucket()).isEqualTo("ufs-demo-avatar");
    }

    @Test
    @DisplayName("head: 不存在抛出 NOT_FOUND")
    void shouldThrowNotFoundOnHeadMissing() {
        when(fileMetaMapper.selectByKey(TENANT, "nonexistent")).thenReturn(null);

        assertThatThrownBy(() -> service.head("nonexistent"))
                .isInstanceOf(UfsException.class)
                .matches(e -> ((UfsException) e).getErrorCode() == UfsErrorCode.NOT_FOUND);
    }

    // ── delete ────────────────────────────────────────────

    @Test
    @DisplayName("delete: 软删除对象")
    void shouldSoftDeleteObject() {
        long now = Instant.now().toEpochMilli();
        FileMeta fm = fileMeta("f_001", "test/photo.jpg", "avatar", 1, now);
        when(fileMetaMapper.selectByKey(TENANT, "test/photo.jpg")).thenReturn(fm);
        when(factory.get("local")).thenReturn(provider);

        service.delete("test/photo.jpg");

        verify(provider).delete(any(StorageProvider.DeleteContext.class));
        verify(fileMetaMapper).softDelete(eq(TENANT), eq("f_001"), any(Long.class));
    }

    // ── copy ──────────────────────────────────────────────

    @Test
    @DisplayName("copy: 复制对象")
    void shouldCopyObject() {
        CopyMoveRequest req = new CopyMoveRequest("ufs-demo-avatar", "src/file.txt", "dest/file.txt");
        when(bucketService.get("ufs-demo-avatar")).thenReturn(DEMO_BUCKET);
        when(factory.get("local")).thenReturn(provider);

        service.copy(req);

        verify(provider).copy(any(StorageProvider.CopyContext.class));
    }

    // ── move ──────────────────────────────────────────────

    @Test
    @DisplayName("move: 移动对象")
    void shouldMoveObject() {
        CopyMoveRequest req = new CopyMoveRequest("ufs-demo-avatar", "src/file.txt", "dest/file.txt");
        when(bucketService.get("ufs-demo-avatar")).thenReturn(DEMO_BUCKET);
        when(factory.get("local")).thenReturn(provider);

        service.move(req);

        verify(provider).move(any(StorageProvider.MoveContext.class));
    }

    // ── helper ────────────────────────────────────────────

    private FileMeta fileMeta(String fileId, String objectKey, String bizType, int status, long now) {
        return new FileMeta(fileId, TENANT, bizType, "local", "ufs-demo-avatar", objectKey,
                "file.dat", 102400L, "application/octet-stream", "d41d8cd98f00b204e9800998ecf8427e",
                null, null, null, null,
                status, null, null, 0,
                now, now, null);
    }
}
