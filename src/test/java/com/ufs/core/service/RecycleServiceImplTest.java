package com.ufs.core.service;

import com.ufs.core.audit.AuditEvent;
import com.ufs.core.audit.AuditPublisher;
import com.ufs.core.common.PageResult;
import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.model.dto.object.ObjectDto;
import com.ufs.core.repository.entity.FileMeta;
import com.ufs.core.repository.mapper.FileMetaMapper;
import com.ufs.core.service.impl.RecycleServiceImpl;
import com.ufs.core.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RecycleService 单元测试")
@ExtendWith(MockitoExtension.class)
class RecycleServiceImplTest {

    @Mock private FileMetaMapper mapper;
    @Mock private AuditPublisher audit;

    private RecycleServiceImpl service;
    private static final String TENANT = "t_demo";
    private static final long NOW = Instant.now().toEpochMilli();

    @BeforeEach
    void setUp() {
        TenantContextHolder.set(new TenantContextHolder.Context(TENANT, "op", "req-01", "app_demo", false));
        service = new RecycleServiceImpl(mapper, audit);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("list: 返回已删除对象列表")
    void shouldListDeletedObjects() {
        FileMeta fm = fileMeta("f_001", "test/file.txt", 1);
        when(mapper.listDeleted(TENANT, 7)).thenReturn(List.of(fm));

        PageResult<ObjectDto> page = service.list(7);

        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).objectKey()).isEqualTo("test/file.txt");
    }

    @Test
    @DisplayName("restore: 成功恢复对象")
    void shouldRestoreObject() {
        FileMeta fm = fileMeta("f_001", "test/file.txt", 1);
        when(mapper.selectById(TENANT, "f_001")).thenReturn(fm);

        service.restore("f_001");

        verify(mapper).restore(eq(TENANT), eq("f_001"), anyLong());
        verify(audit).publish(any(AuditEvent.class));
    }

    @Test
    @DisplayName("restore: 对象不存在抛出 NOT_FOUND")
    void shouldThrowNotFoundOnRestoreMissing() {
        when(mapper.selectById(TENANT, "f_none")).thenReturn(null);

        assertThatThrownBy(() -> service.restore("f_none"))
                .isInstanceOf(UfsException.class)
                .matches(e -> ((UfsException) e).getErrorCode() == UfsErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("purge: 成功彻底删除对象")
    void shouldPurgeObject() {
        FileMeta fm = fileMeta("f_001", "test/file.txt", 1);
        when(mapper.selectById(TENANT, "f_001")).thenReturn(fm);

        service.purge("f_001");

        verify(mapper).purge(TENANT, "f_001");
        verify(audit).publish(any(AuditEvent.class));
    }

    @Test
    @DisplayName("purge: 对象不存在抛出 NOT_FOUND")
    void shouldThrowNotFoundOnPurgeMissing() {
        when(mapper.selectById(TENANT, "f_none")).thenReturn(null);

        assertThatThrownBy(() -> service.purge("f_none"))
                .isInstanceOf(UfsException.class)
                .matches(e -> ((UfsException) e).getErrorCode() == UfsErrorCode.NOT_FOUND);
    }

    private FileMeta fileMeta(String fileId, String objectKey, int status) {
        return new FileMeta(fileId, TENANT, "avatar", "local", "ufs-demo-avatar", objectKey,
                "test.png", 102400L, "image/png", "d41d8cd98f00b204e9800998ecf8427e",
                null, null, null, null,
                status, null, null, 0,
                NOW, NOW, null);
    }
}
