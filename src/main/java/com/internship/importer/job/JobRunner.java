package com.internship.importer.job;

import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
@AllArgsConstructor
public class JobRunner {

    private final JobLoader jobLoader;
    private final ExecutorService executorService;

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
