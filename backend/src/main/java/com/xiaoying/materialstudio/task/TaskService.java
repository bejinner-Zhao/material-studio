package com.xiaoying.materialstudio.task;

import com.xiaoying.materialstudio.asset.UploadAsset;
import com.xiaoying.materialstudio.common.BadRequestException;
import com.xiaoying.materialstudio.common.NotFoundException;
import com.xiaoying.materialstudio.mapper.TaskAssetMapper;
import com.xiaoying.materialstudio.mapper.TaskMapper;
import com.xiaoying.materialstudio.mapper.TemplateMapper;
import com.xiaoying.materialstudio.mapper.UploadAssetMapper;
import com.xiaoying.materialstudio.template.Template;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskAssetMapper taskAssetMapper;
    private final TemplateMapper templateMapper;
    private final UploadAssetMapper uploadAssetMapper;

    public TaskService(TaskMapper taskMapper,
                       TaskAssetMapper taskAssetMapper,
                       TemplateMapper templateMapper,
                       UploadAssetMapper uploadAssetMapper) {
        this.taskMapper = taskMapper;
        this.taskAssetMapper = taskAssetMapper;
        this.templateMapper = templateMapper;
        this.uploadAssetMapper = uploadAssetMapper;
    }

    /**
     * 只做校验与落库，不做任何制作动作。事务提交后由调度器取走执行。
     */
    @Transactional
    public TaskResponse create(Long userId, CreateTaskRequest request) {
        Template template = templateMapper.findById(request.templateId())
                .orElseThrow(() -> new NotFoundException("模板不存在：" + request.templateId()));

        List<Long> assetIds = request.assetIds().stream().distinct().toList();
        if (assetIds.size() != request.assetIds().size()) {
            throw new BadRequestException("素材列表中存在重复项");
        }
        if (assetIds.size() != template.getRequiredImageCount()) {
            throw new BadRequestException("该模板需要 " + template.getRequiredImageCount()
                    + " 张图片，本次提交 " + assetIds.size() + " 张");
        }

        List<UploadAsset> assets = uploadAssetMapper.findByIdsAndUserId(assetIds, userId);
        if (assets.size() != assetIds.size()) {
            throw new BadRequestException("部分素材不存在或不属于当前用户");
        }
        boolean mismatched = assets.stream()
                .anyMatch(asset -> !template.getId().equals(asset.getTemplateId()));
        if (mismatched) {
            throw new BadRequestException("素材与所选模板不匹配");
        }

        CreationTask task = new CreationTask();
        task.setUserId(userId);
        task.setTemplateId(template.getId());
        task.setTemplateTitle(template.getTitle());
        task.setTemplateCoverUrl(template.getCoverUrl());
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        for (int i = 0; i < assetIds.size(); i++) {
            taskAssetMapper.insert(task.getId(), assetIds.get(i), i);
        }
        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long userId, Long taskId) {
        return taskMapper.findByIdAndUserId(taskId, userId)
                .map(TaskResponse::from)
                .orElseThrow(() -> new NotFoundException("任务不存在：" + taskId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 1);
        return taskMapper.findByUserId(userId, safeSize, (safePage - 1) * safeSize).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listActive(Long userId) {
        return taskMapper.findActiveByUserId(userId).stream()
                .map(TaskResponse::from)
                .toList();
    }
}
