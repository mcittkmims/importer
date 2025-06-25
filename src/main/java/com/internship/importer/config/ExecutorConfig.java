package com.internship.importer.config;

import com.internship.importer.core.job.JobLoader;
import com.internship.importer.infrastructure.persistence.JobConfigurationLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Autowired
    private JobConfigurationLoader jobLoader;
    
    private ExecutorService jobExecutorService;

    @PostConstruct
    public void initializeExecutor() {
        jobLoader.loadJobConfigs();
        int jobCount = jobLoader.getCurrentScannedJobs();
        int poolSize = Math.max(1, jobCount);
        this.jobExecutorService = Executors.newFixedThreadPool(poolSize);
    }

    @Bean(name = "executorService", destroyMethod = "shutdown")
    public ExecutorService executorService() {
        return this.jobExecutorService;
    }

    @Bean(name = "exportExecutorService", destroyMethod = "shutdown")
    public ExecutorService exportExecutorService() {
        return Executors.newFixedThreadPool(10);
    }
}
