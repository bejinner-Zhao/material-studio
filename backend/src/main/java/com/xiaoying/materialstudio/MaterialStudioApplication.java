package com.xiaoying.materialstudio;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.xiaoying.materialstudio.mapper")
public class MaterialStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaterialStudioApplication.class, args);
    }
}
