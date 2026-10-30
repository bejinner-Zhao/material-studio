package com.xiaoying.materialstudio.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTaskRequest(
        @NotBlank(message = "请指定模板") String templateId,
        @NotEmpty(message = "请先上传素材") List<@NotNull(message = "素材 id 不能为空") Long> assetIds
) {
}
