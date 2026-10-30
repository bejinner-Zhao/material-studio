package com.xiaoying.materialstudio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StorageProperties storage;

    public WebConfig(StorageProperties storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 用户上传的素材与生成结果从磁盘提供；模板封面走 classpath 静态资源（/demo/**）
        registry.addResourceHandler("/files/**")
                .addResourceLocations(storage.rootUri())
                .setCachePeriod(3600);
    }
}
