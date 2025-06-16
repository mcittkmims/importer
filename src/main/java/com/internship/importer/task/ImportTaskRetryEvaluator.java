package com.internship.importer.task;

import com.internship.importer.handler.file.StatusFileHandler;

import java.time.Instant;
import java.util.List;


public class ImportTaskRetryEvaluator  implements TaskRetryEvaluator{
    private ImportTask importTask;
    private List<Task> tasks;
    private StatusFileHandler statusFileHandler;
    private int expirationSeconds;

    public ImportTaskRetryEvaluator(int expirationSeconds, StatusFileHandler statusFileHandler, ImportTask importTask, Task...tasks){
        this.importTask = importTask;
        this.statusFileHandler = statusFileHandler;
        this.tasks = List.of(tasks);
        this.expirationSeconds = expirationSeconds;
    }

    public void evaluateTasks(){
        if(importTask.getStatus() == TaskStatus.COMPLETE){
            Instant fileCreationTime = statusFileHandler.getStatusFileCreationTime(importTask.getJobName(), importTask.getTaskName());
            if(!fileCreationTime.isBefore(Instant.now().minusSeconds(expirationSeconds))){
                return;
            }
            importTask.setToCompleteStatus();
        }
        for(Task task: tasks){
            task.setToIncompleteStatus();
        }
    }

}
