package com.xiaoying.materialstudio.task;

/** 任务整体状态：终态为 SUCCEEDED / FAILED，进入终态后不再变化 */
public enum TaskStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }
}
