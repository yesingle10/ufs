package com.ufs.core.model.dto.object;

import jakarta.validation.constraints.NotBlank;

/**
 * 复制/移动请求（对齐 OpenAPI Object.Copy / Object.Move）。
 */
public record CopyMoveRequest(
        @NotBlank String bucket,
        @NotBlank String srcObjectKey,
        @NotBlank String destObjectKey) {
}
