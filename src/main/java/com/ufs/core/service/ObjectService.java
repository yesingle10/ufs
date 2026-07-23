package com.ufs.core.service;

import com.ufs.core.common.PageResult;
import com.ufs.core.model.dto.object.CompleteUploadRequest;
import com.ufs.core.model.dto.object.CopyMoveRequest;
import com.ufs.core.model.dto.object.ListObjectsRequest;
import com.ufs.core.model.dto.object.ObjectDto;
import com.ufs.core.model.dto.object.PresignUploadRequest;
import com.ufs.core.model.dto.object.PresignUploadResponse;

/**
 * 对象业务接口（对齐 OpenAPI Object tag）：预签名直传、分片完成、列举、HEAD、删除、复制、移动。
 */
public interface ObjectService {

    PresignUploadResponse presignUpload(PresignUploadRequest req);

    ObjectDto completeUpload(CompleteUploadRequest req);

    PageResult<ObjectDto> list(ListObjectsRequest req);

    ObjectDto head(String objectKey);

    void delete(String objectKey);

    void copy(CopyMoveRequest req);

    void move(CopyMoveRequest req);
}
