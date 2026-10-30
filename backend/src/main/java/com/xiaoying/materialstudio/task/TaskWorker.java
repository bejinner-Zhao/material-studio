package com.xiaoying.materialstudio.task;

import com.xiaoying.materialstudio.asset.UploadAsset;
import com.xiaoying.materialstudio.config.StorageProperties;
import com.xiaoying.materialstudio.mapper.TaskAssetMapper;
import com.xiaoying.materialstudio.mapper.TaskMapper;
import com.xiaoying.materialstudio.mapper.TaskStageLogMapper;
import com.xiaoying.materialstudio.mapper.UploadAssetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 任务执行体：素材检查 → 模拟生成 → 结果整理。
 * 每个阶段都有独立的开始/结束日志，任一步失败即停止后续步骤。
 */
@Component
public class TaskWorker {

    private static final Logger log = LoggerFactory.getLogger(TaskWorker.class);
    private static final int RESULT_VARIANT_COUNT = 4;

    private final TaskMapper taskMapper;
    private final TaskAssetMapper taskAssetMapper;
    private final TaskStageLogMapper stageLogMapper;
    private final UploadAssetMapper uploadAssetMapper;
    private final StorageProperties storage;
    private final SimulatorSettings simulator;

    public TaskWorker(TaskMapper taskMapper,
                      TaskAssetMapper taskAssetMapper,
                      TaskStageLogMapper stageLogMapper,
                      UploadAssetMapper uploadAssetMapper,
                      StorageProperties storage,
                      SimulatorSettings simulator) {
        this.taskMapper = taskMapper;
        this.taskAssetMapper = taskAssetMapper;
        this.stageLogMapper = stageLogMapper;
        this.uploadAssetMapper = uploadAssetMapper;
        this.storage = storage;
        this.simulator = simulator;
    }

    public void run(Long taskId) {
        CreationTask task = taskMapper.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("任务不存在，跳过执行: {}", taskId);
            return;
        }
        if (task.getStatus().isTerminal()) {
            log.info("任务已是终态，跳过重复执行: {} status={}", taskId, task.getStatus());
            return;
        }

        TaskStage current = TaskStage.MATERIAL_CHECK;
        String resultUrl = null;
        try {
            List<Long> assetIds = taskAssetMapper.findAssetIds(taskId);

            current = TaskStage.MATERIAL_CHECK;
            beginStage(taskId, current);
            simulateDelay();
            failIfConfigured(current);
            checkAssets(task.getUserId(), assetIds);
            writeLog(taskId, current, StageLogStatus.SUCCEEDED, "共校验 " + assetIds.size() + " 张素材");

            current = TaskStage.GENERATING;
            beginStage(taskId, current);
            simulateDelay();
            failIfConfigured(current);
            resultUrl = produceResult(taskId);
            writeLog(taskId, current, StageLogStatus.SUCCEEDED, "生成完成：" + resultUrl);

            current = TaskStage.FINALIZING;
            beginStage(taskId, current);
            simulateDelay();
            failIfConfigured(current);
            taskMapper.markSucceeded(taskId, resultUrl);
            writeLog(taskId, current, StageLogStatus.SUCCEEDED, "结果已整理");

            log.info("任务完成 taskId={} result={}", taskId, resultUrl);
        } catch (StageFailure failure) {
            fail(taskId, failure.getStage(), failure.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail(taskId, current, "任务执行被中断");
        } catch (Exception ex) {
            log.error("任务执行异常 taskId={} stage={}", taskId, current, ex);
            fail(taskId, current, "处理过程中出现异常：" + ex.getClass().getSimpleName());
        }
    }

    private void beginStage(Long taskId, TaskStage stage) {
        taskMapper.updateStage(taskId, stage);
        writeLog(taskId, stage, StageLogStatus.STARTED, null);
    }

    private void checkAssets(Long userId, List<Long> assetIds) {
        if (assetIds.isEmpty()) {
            throw new StageFailure(TaskStage.MATERIAL_CHECK, "任务没有关联素材");
        }
        List<UploadAsset> assets = uploadAssetMapper.findByIdsAndUserId(assetIds, userId);
        if (assets.size() != assetIds.size()) {
            throw new StageFailure(TaskStage.MATERIAL_CHECK, "部分素材记录已被删除");
        }
        for (UploadAsset asset : assets) {
            Path path = storage.rootPath().resolve(asset.getStoredPath());
            if (!Files.isReadable(path)) {
                throw new StageFailure(TaskStage.MATERIAL_CHECK,
                        "素材文件不可读：" + asset.getOriginalName());
            }
        }
    }

    private String produceResult(Long taskId) throws IOException {
        int variant = Math.floorMod(taskId, RESULT_VARIANT_COUNT) + 1;
        ClassPathResource source = new ClassPathResource("static/demo/results/result-" + variant + ".svg");

        Path directory = storage.rootPath().resolve("results");
        Files.createDirectories(directory);
        Path target = directory.resolve(taskId + ".svg");
        try (InputStream in = source.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/files/results/" + taskId + ".svg";
    }

    private void simulateDelay() throws InterruptedException {
        long delay = simulator.getStageDelayMs();
        if (delay > 0) {
            Thread.sleep(delay);
        }
    }

    private void failIfConfigured(TaskStage stage) {
        if (simulator.getFailStage() == stage) {
            throw new StageFailure(stage, "模拟失败：" + stageLabel(stage) + "阶段被配置为失败");
        }
    }

    private void fail(Long taskId, TaskStage stage, String message) {
        log.info("任务失败 taskId={} stage={} reason={}", taskId, stage, message);
        writeLog(taskId, stage, StageLogStatus.FAILED, message);
        taskMapper.markFailed(taskId, stage, message);
    }

    private void writeLog(Long taskId, TaskStage stage, StageLogStatus status, String message) {
        TaskStageLog entry = new TaskStageLog();
        entry.setTaskId(taskId);
        entry.setStage(stage);
        entry.setStatus(status);
        entry.setMessage(message);
        stageLogMapper.insert(entry);
    }

    private static String stageLabel(TaskStage stage) {
        return switch (stage) {
            case MATERIAL_CHECK -> "素材检查";
            case GENERATING -> "模拟生成";
            case FINALIZING -> "结果整理";
        };
    }

    private static class StageFailure extends RuntimeException {

        private final TaskStage stage;

        StageFailure(TaskStage stage, String message) {
            super(message);
            this.stage = stage;
        }

        TaskStage getStage() {
            return stage;
        }
    }
}
