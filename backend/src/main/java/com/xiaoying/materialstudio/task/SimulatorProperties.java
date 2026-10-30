package com.xiaoying.materialstudio.task;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.simulator")
public class SimulatorProperties {

    /** NONE / MATERIAL_CHECK / GENERATING / FINALIZING */
    private String failStage = "NONE";

    private long stageDelayMs = 3000;

    private int maxConcurrent = 2;

    private long pollIntervalMs = 300;

    /** 是否开放运行时调整模拟参数的开发接口（仅用于演示与自测） */
    private boolean devApiEnabled = true;

    public String getFailStage() {
        return failStage;
    }

    public void setFailStage(String failStage) {
        this.failStage = failStage;
    }

    public long getStageDelayMs() {
        return stageDelayMs;
    }

    public void setStageDelayMs(long stageDelayMs) {
        this.stageDelayMs = stageDelayMs;
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public boolean isDevApiEnabled() {
        return devApiEnabled;
    }

    public void setDevApiEnabled(boolean devApiEnabled) {
        this.devApiEnabled = devApiEnabled;
    }
}
