package com.internship.importer.core.job;

import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
public class JobRunner {

    private final JobLoader jobLoader;
    @Qualifier("executorService")
    private final ExecutorService executorService;

    public JobRunner(JobLoader jobLoader, ExecutorService executorService) {
        this.jobLoader = jobLoader;
        this.executorService = executorService;
    }

    public void run() {
        for (Job job : jobLoader.getJobs()) {
            executorService.submit(job::execute);
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }
}
