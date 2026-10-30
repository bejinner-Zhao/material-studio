package com.xiaoying.materialstudio.task;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String templateId,
        String templateTitle,
        String templateCoverUrl,
        TaskStatus status,
        TaskStage stage,
        TaskStage errorStage,
        String errorMessage,
        String resultUrl,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    public static TaskResponse from(CreationTask task) {
        return new TaskResponse(
                task.getId(),
                task.getTemplateId(),
                task.getTemplateTitle(),
                task.getTemplateCoverUrl(),
                task.getStatus(),
                task.getStage(),
                task.getErrorStage(),
                task.getErrorMessage(),
                task.getResultUrl(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getFinishedAt()
        );
    }
}
