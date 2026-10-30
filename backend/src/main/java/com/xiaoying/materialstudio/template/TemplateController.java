package com.xiaoying.materialstudio.template;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public TemplateListResponse list(@RequestParam(name = "category", required = false) String category) {
        return templateService.list(category);
    }

    @GetMapping("/{id}")
    public TemplateResponse detail(@PathVariable("id") String id) {
        return templateService.get(id);
    }
}
