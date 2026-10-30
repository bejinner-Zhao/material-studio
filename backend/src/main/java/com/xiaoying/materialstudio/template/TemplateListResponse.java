package com.xiaoying.materialstudio.template;

import java.util.List;

/**
 * 列表接口一次返回分类与模板，前端首屏只需一次请求。
 */
public record TemplateListResponse(
        List<String> categories,
        List<TemplateResponse> templates
) {
}
