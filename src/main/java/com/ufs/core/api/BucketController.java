package com.ufs.core.api;

import com.ufs.core.common.ApiResponse;
import com.ufs.core.config.Idempotent;
import com.ufs.core.model.dto.bucket.BucketDto;
import com.ufs.core.model.dto.bucket.CreateBucketRequest;
import com.ufs.core.model.dto.bucket.UpdateBucketRequest;
import com.ufs.core.service.BucketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 存储桶接口（对齐 OpenAPI Bucket tag）。统一返回 ApiResponse 信封。
 */
@RestController
@RequestMapping("/v1/buckets")
@Tag(name = "Buckets", description = "存储桶管理")
public class BucketController {

    private final BucketService service;

    public BucketController(BucketService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<BucketDto>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{name}")
    public ApiResponse<BucketDto> get(@PathVariable String name) {
        return ApiResponse.ok(service.get(name));
    }

    @PostMapping
    @Idempotent
    public ApiResponse<BucketDto> create(@Valid @RequestBody CreateBucketRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/{name}")
    public ApiResponse<BucketDto> update(@PathVariable String name, @Valid @RequestBody UpdateBucketRequest req) {
        return ApiResponse.ok(service.update(name, req));
    }

    @DeleteMapping("/{name}")
    public ApiResponse<Void> delete(@PathVariable String name) {
        service.delete(name);
        return ApiResponse.ok();
    }
}
