package com.xiaoying.materialstudio.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;

@Component
public class StorageInitializer implements ApplicationRunner {

    private final StorageProperties storage;

    public StorageInitializer(StorageProperties storage) {
        this.storage = storage;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Files.createDirectories(storage.rootPath().resolve("uploads"));
        Files.createDirectories(storage.rootPath().resolve("results"));
    }
}
