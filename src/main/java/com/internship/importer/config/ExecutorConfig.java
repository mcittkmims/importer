package com.internship.importer.config;

import com.internship.importer.job.JobLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Configuration
public class ExecutorConfig {

    private final JobLoader jobLoader;

    public ExecutorConfig(JobLoader jobLoader) {
        this.jobLoader = jobLoader;
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(jobLoader.getJobs().size());
    }
}
