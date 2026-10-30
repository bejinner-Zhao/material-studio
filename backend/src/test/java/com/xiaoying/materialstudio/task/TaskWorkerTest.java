package com.xiaoying.materialstudio.task;

import com.xiaoying.materialstudio.asset.UploadAsset;
import com.xiaoying.materialstudio.config.StorageProperties;
import com.xiaoying.materialstudio.mapper.TaskAssetMapper;
import com.xiaoying.materialstudio.mapper.TaskMapper;
import com.xiaoying.materialstudio.mapper.TaskStageLogMapper;
import com.xiaoying.materialstudio.mapper.UploadAssetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskWorkerTest {

    @TempDir
    Path tempDir;

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskAssetMapper taskAssetMapper;
    @Mock
    private TaskStageLogMapper stageLogMapper;
    @Mock
    private UploadAssetMapper uploadAssetMapper;
    @Mock
    private StorageProperties storage;

    private SimulatorSettings simulator;
    private TaskWorker worker;

    @BeforeEach
    void setUp() {
        simulator = new SimulatorSettings(new SimulatorProperties());
        simulator.setStageDelayMs(0);
        worker = new TaskWorker(taskMapper, taskAssetMapper, stageLogMapper,
                uploadAssetMapper, storage, simulator);
        when(storage.rootPath()).thenReturn(tempDir);
    }

    @Test
    @DisplayName("成功路径：三个阶段按顺序执行，最终标记成功")
    void runsStagesInOrderAndMarksSucceeded() throws IOException {
        stubTask(TaskStatus.PENDING);
        stubAssets();

        worker.run(1L);

        List<TaskStageLog> logs = captureLogs();
        assertThat(startedStages(logs))
                .containsExactly(TaskStage.MATERIAL_CHECK, TaskStage.GENERATING, TaskStage.FINALIZING);
        assertThat(logs).allMatch(log -> log.getStatus() != StageLogStatus.FAILED);

        verify(taskMapper).markSucceeded(eq(1L), startsWith("/files/results/"));
        verify(taskMapper, never()).markFailed(anyLong(), any(), any());
        assertThat(tempDir.resolve("results")).isDirectory();
    }

    @Test
    @DisplayName("失败中断：生成阶段失败后不再执行结果整理")
    void stopsAfterConfiguredFailure() {
        simulator.setFailStage(TaskStage.GENERATING);
        stubTask(TaskStatus.PENDING);
        stubAssets();

        worker.run(1L);

        List<TaskStageLog> logs = captureLogs();
        assertThat(logs).noneMatch(log -> log.getStage() == TaskStage.FINALIZING);
        assertThat(logs).anyMatch(log ->
                log.getStage() == TaskStage.GENERATING && log.getStatus() == StageLogStatus.FAILED);

        verify(taskMapper).markFailed(1L, TaskStage.GENERATING, "模拟失败：模拟生成阶段被配置为失败");
        verify(taskMapper, never()).markSucceeded(anyLong(), any());
        assertThat(tempDir.resolve("results")).doesNotExist();
    }

    @Test
    @DisplayName("素材缺失：在素材检查阶段失败终止")
    void failsWhenAssetFileMissing() {
        stubTask(TaskStatus.PENDING);
        when(taskAssetMapper.findAssetIds(1L)).thenReturn(List.of(10L));
        // 故意不在磁盘上创建文件
        when(uploadAssetMapper.findByIdsAndUserId(List.of(10L), 1L))
                .thenReturn(List.of(asset(10L, "uploads/1/missing.png")));

        worker.run(1L);

        verify(taskMapper).markFailed(eq(1L), eq(TaskStage.MATERIAL_CHECK), contains("素材文件不可读"));
        verify(taskMapper, never()).markSucceeded(anyLong(), any());
    }

    @Test
    @DisplayName("幂等：终态任务不会被重复执行")
    void skipsTerminalTask() {
        when(taskMapper.findById(1L)).thenReturn(Optional.of(task(TaskStatus.SUCCEEDED)));

        worker.run(1L);

        verifyNoInteractions(stageLogMapper, taskAssetMapper);
        verify(taskMapper, never()).markSucceeded(anyLong(), any());
        verify(taskMapper, never()).markFailed(anyLong(), any(), any());
    }

    private void stubTask(TaskStatus status) {
        when(taskMapper.findById(1L)).thenReturn(Optional.of(task(status)));
    }

    private void stubAssets() {
        when(taskAssetMapper.findAssetIds(1L)).thenReturn(List.of(10L, 11L));
        createFile("uploads/1/a.png");
        createFile("uploads/1/b.png");
        when(uploadAssetMapper.findByIdsAndUserId(List.of(10L, 11L), 1L))
                .thenReturn(List.of(asset(10L, "uploads/1/a.png"), asset(11L, "uploads/1/b.png")));
    }

    private void createFile(String relativePath) {
        Path target = tempDir.resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, "demo-bytes");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<TaskStageLog> captureLogs() {
        ArgumentCaptor<TaskStageLog> captor = ArgumentCaptor.forClass(TaskStageLog.class);
        verify(stageLogMapper, atLeastOnce()).insert(captor.capture());
        return captor.getAllValues();
    }

    private static List<TaskStage> startedStages(List<TaskStageLog> logs) {
        return logs.stream()
                .filter(log -> log.getStatus() == StageLogStatus.STARTED)
                .map(TaskStageLog::getStage)
                .toList();
    }

    private static CreationTask task(TaskStatus status) {
        CreationTask task = new CreationTask();
        task.setId(1L);
        task.setUserId(1L);
        task.setTemplateId("demo-postcard");
        task.setTemplateTitle("旅行明信片");
        task.setTemplateCoverUrl("/demo/covers/postcard.svg");
        task.setStatus(status);
        return task;
    }

    private static UploadAsset asset(long id, String storedPath) {
        UploadAsset asset = new UploadAsset();
        asset.setId(id);
        asset.setUserId(1L);
        asset.setStoredPath(storedPath);
        asset.setOriginalName(storedPath.substring(storedPath.lastIndexOf('/') + 1));
        return asset;
    }
}
