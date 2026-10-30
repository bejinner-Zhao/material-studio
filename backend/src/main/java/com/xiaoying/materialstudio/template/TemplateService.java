package com.xiaoying.materialstudio.template;

import com.xiaoying.materialstudio.common.NotFoundException;
import com.xiaoying.materialstudio.mapper.TemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TemplateService {

    private final TemplateMapper templateMapper;

    public TemplateService(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public TemplateListResponse list(String category) {
        List<TemplateResponse> templates = templateMapper.findAll(normalizeCategory(category)).stream()
                .map(TemplateResponse::from)
                .toList();
        return new TemplateListResponse(templateMapper.findCategories(), templates);
    }

    public TemplateResponse get(String id) {
        return templateMapper.findById(id)
                .map(TemplateResponse::from)
                .orElseThrow(() -> new NotFoundException("模板不存在：" + id));
    }

    /** 空值、"all" 与 "全部" 都表示不筛选 */
    private static String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }
        String trimmed = category.trim();
        if (trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed) || "全部".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }
}
