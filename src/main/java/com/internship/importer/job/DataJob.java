package com.internship.importer.job;

import com.internship.importer.task.Task;
import com.internship.importer.task.TaskRetryEvaluator;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class DataJob  implements Job{
    private List<Task> tasks;
    private TaskRetryEvaluator retryEvaluator;
    private final String jobName;

    public DataJob(TaskRetryEvaluator retryEvaluator, String jobName, Task...tasks){
        this.retryEvaluator = retryEvaluator;
        this.tasks = List.of(tasks);
        this.jobName = jobName;
    }

    public void execute(){
        retryEvaluator.evaluateTasks();
        for (Task task : tasks){
            task.execute();
        }
        log.info("Job {} cpmpleted!", jobName);
    }
}
