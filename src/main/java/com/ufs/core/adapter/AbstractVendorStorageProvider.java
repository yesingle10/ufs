package com.ufs.core.adapter;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import com.ufs.core.spi.InitUploadResult;
import com.ufs.core.spi.ObjectMeta;
import com.ufs.core.spi.PresignResult;
import com.ufs.core.spi.StorageProvider;

import java.util.List;
import java.util.Map;

/**
 * 厂商适配器基类（占位）。真实实现需引入对应云厂商 SDK（OSS/Kodo/TOS），
 * 此处统一抛“未配置”，由接入阶段补齐（对齐 Java 规范 §6、PRD §9 多厂商适配）。
 */
public abstract class AbstractVendorStorageProvider implements StorageProvider {

    protected UfsException notConfigured() {
        return new UfsException(UfsErrorCode.VENDOR_ERROR,
                Map.of("vendor", vendor(), "reason", "provider not configured; integrate vendor SDK"));
    }

    @Override
    public InitUploadResult initUpload(InitUploadContext ctx) {
        throw notConfigured();
    }

    @Override
    public PresignResult presignUpload(PresignContext ctx) {
        throw notConfigured();
    }

    @Override
    public PresignResult presignPartUpload(InitUploadContext ctx, String uploadId, int partNumber) {
        throw notConfigured();
    }

    @Override
    public PresignResult presignDownload(PresignContext ctx) {
        throw notConfigured();
    }

    @Override
    public ObjectMeta completeUpload(CompleteContext ctx) {
        throw notConfigured();
    }

    @Override
    public List<ObjectMeta> list(ListContext ctx) {
        throw notConfigured();
    }

    @Override
    public ObjectMeta head(HeadContext ctx) {
        throw notConfigured();
    }

    @Override
    public void copy(CopyContext ctx) {
        throw notConfigured();
    }

    @Override
    public void move(MoveContext ctx) {
        throw notConfigured();
    }

    @Override
    public void delete(DeleteContext ctx) {
        throw notConfigured();
    }
}
