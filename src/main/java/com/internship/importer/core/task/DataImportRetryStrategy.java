package com.internship.importer.core.task;

import com.internship.importer.infrastructure.persistence.TaskStatusManager;

import java.time.Instant;
import java.util.List;


public class DataImportRetryStrategy implements TaskRetryStrategy{
    private DataImportTask dataImportTask;
    private List<Task> tasks;
    private TaskStatusManager taskStatusManager;
    private int expirationSeconds;

    public DataImportRetryStrategy(int expirationSeconds, TaskStatusManager taskStatusManager, DataImportTask dataImportTask, Task...tasks){
        this.dataImportTask = dataImportTask;
        this.taskStatusManager = taskStatusManager;
        this.tasks = List.of(tasks);
        this.expirationSeconds = expirationSeconds;
    }

    public void evaluateTasks(){
        if(dataImportTask.getStatus() == TaskStatus.COMPLETE){
            Instant fileCreationTime = taskStatusManager.getStatusFileCreationTime(dataImportTask.getJobName(), dataImportTask.getTaskName());
            if(!fileCreationTime.isBefore(Instant.now().minusSeconds(expirationSeconds))){
                return;
            }
            dataImportTask.setToIncompleteStatus();
        }
        for(Task task: tasks){
            task.setToIncompleteStatus();
        }
    }

}
