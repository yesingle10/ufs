package com.ufs.core.api;

import com.ufs.core.common.ApiResponse;
import com.ufs.core.common.PageResult;
import com.ufs.core.config.Idempotent;
import com.ufs.core.model.dto.object.CompleteUploadRequest;
import com.ufs.core.model.dto.object.CopyMoveRequest;
import com.ufs.core.model.dto.object.ListObjectsRequest;
import com.ufs.core.model.dto.object.ObjectDto;
import com.ufs.core.model.dto.object.PresignUploadRequest;
import com.ufs.core.model.dto.object.PresignUploadResponse;
import com.ufs.core.service.ObjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对象接口（对齐 OpenAPI Object tag）：预签名直传、分片完成、列举、HEAD、删除、复制、移动。
 */
@RestController
@RequestMapping("/v1/objects")
public class ObjectController {

    private final ObjectService service;

    public ObjectController(ObjectService service) {
        this.service = service;
    }

    @PostMapping("/presign")
    public ApiResponse<PresignUploadResponse> presign(@Valid @RequestBody PresignUploadRequest req) {
        return ApiResponse.ok(service.presignUpload(req));
    }

    @PostMapping("/complete")
    @Idempotent
    public ApiResponse<ObjectDto> complete(@Valid @RequestBody CompleteUploadRequest req) {
        return ApiResponse.ok(service.completeUpload(req));
    }

    @GetMapping
    public ApiResponse<PageResult<ObjectDto>> list(@RequestParam(required = false) String bizType,
                                                   @RequestParam(required = false) String provider,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) String cursor,
                                                   @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(service.list(ListObjectsRequest.of(bizType, provider, status, cursor, limit)));
    }

    @GetMapping("/head")
    public ApiResponse<ObjectDto> head(@RequestParam String objectKey) {
        return ApiResponse.ok(service.head(objectKey));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@RequestParam String objectKey) {
        service.delete(objectKey);
        return ApiResponse.ok();
    }

    @PostMapping("/copy")
    public ApiResponse<Void> copy(@Valid @RequestBody CopyMoveRequest req) {
        service.copy(req);
        return ApiResponse.ok();
    }

    @PostMapping("/move")
    public ApiResponse<Void> move(@Valid @RequestBody CopyMoveRequest req) {
        service.move(req);
        return ApiResponse.ok();
    }
}
