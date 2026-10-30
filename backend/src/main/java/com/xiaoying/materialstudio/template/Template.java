package com.xiaoying.materialstudio.template;

import java.util.Arrays;
import java.util.List;

/**
 * 模板持久化对象，字段与 template 表一一对应。
 * acceptedMimeTypes 在库中是逗号分隔字符串，对外由 {@link TemplateResponse} 拆成数组。
 */
public class Template {

    private String id;
    private String title;
    private String category;
    private String coverUrl;
    private String description;
    private int requiredImageCount;
    private String acceptedMimeTypes;
    private long maxFileSizeBytes;
    private int sortOrder;
    private boolean enabled;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRequiredImageCount() {
        return requiredImageCount;
    }

    public void setRequiredImageCount(int requiredImageCount) {
        this.requiredImageCount = requiredImageCount;
    }

    public String getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public void setAcceptedMimeTypes(String acceptedMimeTypes) {
        this.acceptedMimeTypes = acceptedMimeTypes;
    }

    /** 把库中的逗号分隔串拆成 MIME 白名单，供上传校验与接口输出共用 */
    public List<String> acceptedMimeTypeList() {
        if (acceptedMimeTypes == null || acceptedMimeTypes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(acceptedMimeTypes.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
