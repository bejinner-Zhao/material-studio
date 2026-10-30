package com.xiaoying.materialstudio.task;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 运行期可调的模拟参数。启动时取自 application.yml，演示过程中可通过开发接口修改。
 */
@Component
public class SimulatorSettings {

    /** null 表示不注入失败 */
    private volatile TaskStage failStage;
    private volatile long stageDelayMs;

    public SimulatorSettings(SimulatorProperties properties) {
        this.failStage = parseStage(properties.getFailStage());
        this.stageDelayMs = properties.getStageDelayMs();
    }

    public TaskStage getFailStage() {
        return failStage;
    }

    public void setFailStage(TaskStage failStage) {
        this.failStage = failStage;
    }

    public long getStageDelayMs() {
        return stageDelayMs;
    }

    public void setStageDelayMs(long stageDelayMs) {
        this.stageDelayMs = stageDelayMs;
    }

    public static TaskStage parseStage(String raw) {
        if (raw == null || raw.isBlank() || "NONE".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        return TaskStage.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
