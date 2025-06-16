package com.internship.importer.scheduler;

import com.internship.importer.job.JobRunner;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@AllArgsConstructor
@Slf4j  // from lombok
public class JobScheduler {
    private final JobRunner jobRunner;

    @Scheduled(fixedDelay = 6000000)
    public void scheduleJobs() {
        log.info("Starting scheduled job runner at {}", Instant.now());
        jobRunner.run();
    }
}

