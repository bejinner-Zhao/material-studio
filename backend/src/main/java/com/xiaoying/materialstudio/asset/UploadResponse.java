package com.xiaoying.materialstudio.asset;

public record UploadResponse(
        Long id,
        String url,
        String mimeType,
        long sizeBytes,
        String originalName
) {
}
