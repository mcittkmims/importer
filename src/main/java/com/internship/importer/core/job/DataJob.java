package com.internship.importer.core.job;

import com.internship.importer.core.task.Task;
import com.internship.importer.core.task.TaskRetryStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class DataJob implements Job {
    private List<Task> tasks;
    private TaskRetryStrategy retryStrategy;
    private final String jobName;

    public DataJob(TaskRetryStrategy retryStrategy, String jobName, Task... tasks) {
        this.retryStrategy = retryStrategy;
        this.tasks = List.of(tasks);
        this.jobName = jobName;
    }

    public void execute() {
        retryStrategy.evaluateTasks();
        log.info("Job {} started!", jobName);
        for (Task task : tasks) {
            task.execute();
        }
        log.info("Job {} completed!", jobName);
    }
}
