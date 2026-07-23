package com.ufs.core.api;

import com.ufs.core.common.ApiResponse;
import com.ufs.core.common.PageResult;
import com.ufs.core.model.dto.object.ObjectDto;
import com.ufs.core.service.RecycleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 回收站接口（对齐 OpenAPI Recycle tag）：列出已删除对象、恢复、彻底删除。
 */
@RestController
@RequestMapping("/v1/recycle")
public class RecycleController {

    private final RecycleService service;

    public RecycleController(RecycleService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<ObjectDto>> list(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(service.list(limit));
    }

    @PostMapping("/restore")
    public ApiResponse<Void> restore(@RequestBody Map<String, String> body) {
        String fileId = body.get("fileId");
        if (fileId == null || fileId.isBlank()) {
            return ApiResponse.fail("UFS.4000", "fileId required", null);
        }
        service.restore(fileId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> purge(@PathVariable String fileId) {
        service.purge(fileId);
        return ApiResponse.ok();
    }
}
