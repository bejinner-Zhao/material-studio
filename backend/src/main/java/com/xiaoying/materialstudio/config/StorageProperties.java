package com.xiaoying.materialstudio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** 上传素材与生成结果的根目录 */
    private String root = "./data";

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public Path rootPath() {
        return Paths.get(root).toAbsolutePath().normalize();
    }

    /** 供 Spring 静态资源定位使用的 file: URI，必须以 / 结尾 */
    public String rootUri() {
        String uri = rootPath().toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }
}
