package com.ufs.core.service;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.model.dto.bucket.BucketDto;
import com.ufs.core.model.dto.bucket.CreateBucketRequest;
import com.ufs.core.model.dto.bucket.UpdateBucketRequest;
import com.ufs.core.service.impl.BucketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BucketService 单元测试")
class BucketServiceImplTest {

    private BucketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BucketServiceImpl();
    }

    // ── list ──────────────────────────────────────────────

    @Test
    @DisplayName("list: 返回 seed 的 6 个演示桶")
    void shouldReturnSixPreSeededBuckets() {
        List<BucketDto> buckets = service.list();
        assertThat(buckets).hasSize(6);
        assertThat(buckets).extracting(BucketDto::vendor)
                .allMatch("local"::equals);
    }

    // ── get ───────────────────────────────────────────────

    @Test
    @DisplayName("get: 存在则返回桶信息")
    void shouldReturnBucketWhenExists() {
        BucketDto b = service.get("ufs-demo-img");
        assertThat(b.name()).isEqualTo("ufs-demo-img");
        assertThat(b.vendor()).isEqualTo("local");
    }

    @Test
    @DisplayName("get: 不存在抛出 NOT_FOUND")
    void shouldThrowNotFoundWhenBucketMissing() {
        assertThatThrownBy(() -> service.get("nonexistent"))
                .isInstanceOf(UfsException.class)
                .matches(e -> ((UfsException) e).getErrorCode() == UfsErrorCode.NOT_FOUND);
    }

    // ── create ────────────────────────────────────────────

    @Test
    @DisplayName("create: 成功创建新桶")
    void shouldCreateBucketSuccessfully() {
        CreateBucketRequest req = new CreateBucketRequest(
                "my-new-bucket", "oss", "us-west-1",
                null, null, true, false, 90, "private", List.of("*"), "test bucket"
        );

        BucketDto created = service.create(req);
        assertThat(created.name()).isEqualTo("my-new-bucket");
        assertThat(created.vendor()).isEqualTo("oss");
        assertThat(created.region()).isEqualTo("us-west-1");
        assertThat(created.versioning()).isTrue();
        assertThat(created.lifecycleDays()).isEqualTo(90);
        assertThat(created.objectCount()).isZero();
        assertThat(created.storageBytes()).isZero();
    }

    @Test
    @DisplayName("create: 重名抛出 CONFLICT")
    void shouldThrowConflictOnDuplicateName() {
        CreateBucketRequest req = new CreateBucketRequest(
                "ufs-demo-img", "oss", "cn-hangzhou",
                null, null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(UfsException.class)
                .matches(e -> ((UfsException) e).getErrorCode() == UfsErrorCode.CONFLICT);
    }

    // ── update ────────────────────────────────────────────

    @Test
    @DisplayName("update: 成功更新部分字段，其余保留原值")
    void shouldUpdatePartialFields() {
        UpdateBucketRequest req = new UpdateBucketRequest(
                "https://cdn.example.com", null, false, null, 180,
                "public-read", null, "updated note"
        );

        BucketDto updated = service.update("ufs-demo-img", req);
        assertThat(updated.cdnDomain()).isEqualTo("https://cdn.example.com");
        assertThat(updated.versioning()).isFalse();
        assertThat(updated.lifecycleDays()).isEqualTo(180);
        assertThat(updated.acl()).isEqualTo("public-read");
        assertThat(updated.note()).isEqualTo("updated note");
        // 未覆盖字段保留原值
        assertThat(updated.vendor()).isEqualTo("local");
        assertThat(updated.region()).isEqualTo("cn-hangzhou");
    }

    @Test
    @DisplayName("update: 桶不存在抛出 NOT_FOUND")
    void shouldThrowNotFoundOnUpdateMissing() {
        UpdateBucketRequest req = new UpdateBucketRequest(
                null, null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> service.update("nonexistent", req))
                .isInstanceOf(UfsException.class)
                .matches(e -> ((UfsException) e).getErrorCode() == UfsErrorCode.NOT_FOUND);
    }

    // ── delete ────────────────────────────────────────────

    @Test
    @DisplayName("delete: 成功删除桶")
    void shouldDeleteBucketSuccessfully() {
        service.delete("ufs-demo-audio");
        // 删后查不到
        assertThatThrownBy(() -> service.get("ufs-demo-audio"))
                .isInstanceOf(UfsException.class);
    }

    @Test
    @DisplayName("delete: 桶不存在抛出 NOT_FOUND")
    void shouldThrowNotFoundOnDeleteMissing() {
        assertThatThrownBy(() -> service.delete("nonexistent"))
                .isInstanceOf(UfsException.class)
                .matches(e -> ((UfsException) e).getErrorCode() == UfsErrorCode.NOT_FOUND);
    }
}
