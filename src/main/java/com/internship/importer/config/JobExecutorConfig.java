package com.internship.importer.config;

import com.internship.importer.core.job.JobLoader;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@AllArgsConstructor
@Configuration
public class JobExecutorConfig {

    private JobLoader jobLoader;

    @Bean(name = "jobExecutorService", destroyMethod = "shutdown")
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(jobLoader.getJobs().size());
    }

}
