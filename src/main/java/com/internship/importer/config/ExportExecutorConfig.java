package com.internship.importer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExportExecutorConfig {
    @Bean(name = "exportExecutorService", destroyMethod = "shutdown")
    public ExecutorService exportExecutorService() {
        return Executors.newFixedThreadPool(12);
    }
}
