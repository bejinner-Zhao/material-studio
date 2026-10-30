package com.xiaoying.materialstudio.task;

import com.xiaoying.materialstudio.asset.UploadAsset;
import com.xiaoying.materialstudio.common.BadRequestException;
import com.xiaoying.materialstudio.common.NotFoundException;
import com.xiaoying.materialstudio.mapper.TaskAssetMapper;
import com.xiaoying.materialstudio.mapper.TaskMapper;
import com.xiaoying.materialstudio.mapper.TemplateMapper;
import com.xiaoying.materialstudio.mapper.UploadAssetMapper;
import com.xiaoying.materialstudio.template.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskAssetMapper taskAssetMapper;
    @Mock
    private TemplateMapper templateMapper;
    @Mock
    private UploadAssetMapper uploadAssetMapper;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskMapper, taskAssetMapper, templateMapper, uploadAssetMapper);
    }

    @Test
    @DisplayName("模板不存在时拒绝提交")
    void rejectsUnknownTemplate() {
        when(templateMapper.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, new CreateTaskRequest("missing", List.of(10L))))
                .isInstanceOf(NotFoundException.class);

        verify(taskMapper, never()).insert(any());
    }

    @Test
    @DisplayName("素材数量与模板要求不一致时拒绝提交")
    void rejectsWrongImageCount() {
        when(templateMapper.findById("demo-postcard")).thenReturn(Optional.of(template(2)));

        assertThatThrownBy(() -> service.create(1L, new CreateTaskRequest("demo-postcard", List.of(10L))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("2 张");

        verify(taskMapper, never()).insert(any());
    }

    @Test
    @DisplayName("素材 id 重复时拒绝提交")
    void rejectsDuplicateAssetIds() {
        when(templateMapper.findById("demo-postcard")).thenReturn(Optional.of(template(2)));

        assertThatThrownBy(() -> service.create(1L,
                new CreateTaskRequest("demo-postcard", List.of(10L, 10L))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("重复");
    }

    @Test
    @DisplayName("素材不属于当前用户时拒绝提交")
    void rejectsAssetOfAnotherUser() {
        when(templateMapper.findById("demo-postcard")).thenReturn(Optional.of(template(2)));
        when(uploadAssetMapper.findByIdsAndUserId(List.of(10L, 11L), 1L))
                .thenReturn(List.of(asset(10L, "demo-postcard")));

        assertThatThrownBy(() -> service.create(1L,
                new CreateTaskRequest("demo-postcard", List.of(10L, 11L))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("不属于当前用户");
    }

    @Test
    @DisplayName("素材来自其他模板时拒绝提交")
    void rejectsAssetFromAnotherTemplate() {
        when(templateMapper.findById("demo-postcard")).thenReturn(Optional.of(template(2)));
        when(uploadAssetMapper.findByIdsAndUserId(List.of(10L, 11L), 1L))
                .thenReturn(List.of(asset(10L, "demo-postcard"), asset(11L, "demo-santa")));

        assertThatThrownBy(() -> service.create(1L,
                new CreateTaskRequest("demo-postcard", List.of(10L, 11L))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("不匹配");
    }

    @Test
    @DisplayName("提交成功时写入 PENDING 任务并保存模板快照")
    void createsPendingTaskWithSnapshot() {
        when(templateMapper.findById("demo-postcard")).thenReturn(Optional.of(template(2)));
        when(uploadAssetMapper.findByIdsAndUserId(List.of(10L, 11L), 1L))
                .thenReturn(List.of(asset(10L, "demo-postcard"), asset(11L, "demo-postcard")));
        when(taskMapper.insert(any(CreationTask.class))).thenAnswer(invocation -> {
            CreationTask inserted = invocation.getArgument(0);
            inserted.setId(99L);
            return 1;
        });

        TaskResponse response = service.create(1L,
                new CreateTaskRequest("demo-postcard", List.of(10L, 11L)));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
        assertThat(response.stage()).isNull();
        assertThat(response.templateTitle()).isEqualTo("旅行明信片");
        assertThat(response.templateCoverUrl()).isEqualTo("/demo/covers/postcard.svg");

        verify(taskAssetMapper).insert(99L, 10L, 0);
        verify(taskAssetMapper).insert(99L, 11L, 1);
    }

    private static Template template(int requiredImageCount) {
        Template template = new Template();
        template.setId("demo-postcard");
        template.setTitle("旅行明信片");
        template.setCoverUrl("/demo/covers/postcard.svg");
        template.setRequiredImageCount(requiredImageCount);
        template.setAcceptedMimeTypes("image/jpeg,image/png");
        template.setMaxFileSizeBytes(5242880L);
        return template;
    }

    private static UploadAsset asset(long id, String templateId) {
        UploadAsset asset = new UploadAsset();
        asset.setId(id);
        asset.setUserId(1L);
        asset.setTemplateId(templateId);
        asset.setStoredPath("uploads/1/" + id + ".png");
        asset.setOriginalName(id + ".png");
        return asset;
    }
}
