package com.xiaoying.materialstudio.mapper;

import com.xiaoying.materialstudio.template.Template;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface TemplateMapper {

    /** category 为 null 时不筛选 */
    List<Template> findAll(@Param("category") String category);

    Optional<Template> findById(@Param("id") String id);

    List<String> findCategories();
}
