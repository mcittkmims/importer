package com.internship.importer.core.job;

import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
@Slf4j
public class JobRunner {

    private final JobLoader jobLoader;
    @Qualifier("jobExecutorService")
    private final ExecutorService jobExecutorService;

    public JobRunner(JobLoader jobLoader, ExecutorService jobExecutorService) {
        this.jobLoader = jobLoader;
        this.jobExecutorService = jobExecutorService;
    }

    public void run() {
        for (Job job : jobLoader.getJobs()) {
            log.info("Starting job");
            jobExecutorService.submit(job::execute);
        }
    }

    @PreDestroy
    public void shutdown() {
        jobExecutorService.shutdown();
    }
}
