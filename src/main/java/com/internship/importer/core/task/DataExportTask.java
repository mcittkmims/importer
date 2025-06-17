package com.internship.importer.core.task;

import com.internship.importer.infrastructure.persistence.TaskStatusManager;
import com.internship.importer.infrastructure.export.DataExporter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataExportTask implements Task {

    private TaskStatusManager taskStatusManager;
    private String jobName;
    private String task = "export";
    private DataExporter dataExporter;
    private String companyJsonData;
    private String industryJsonData;
    private String tableName;

    public DataExportTask(TaskStatusManager taskStatusManager, String jobName, DataExporter dataExporter,
            String companyJsonData, String industryJsonData, String tableName) {
        this.taskStatusManager = taskStatusManager;
        this.jobName = jobName;
        this.dataExporter = dataExporter;
        this.companyJsonData = companyJsonData;
        this.industryJsonData = industryJsonData;
        this.tableName = tableName;
    }

    @Override
    public TaskStatus getStatus() {
        if (taskStatusManager.statusFileExists(jobName, task)) {
            return TaskStatus.COMPLETE;
        }
        return TaskStatus.INCOMPLETE;
    }

    @Override
    public void setToIncompleteStatus() {
        taskStatusManager.deleteStatusFileIfExists(jobName, task);
    }

    @Override
    public void setToCompleteStatus() {
        taskStatusManager.createSuccessStatusFile(jobName, task);
    }

    @Override
    public void execute() {
        TaskStatus status = this.getStatus();
        if (status == TaskStatus.COMPLETE) {
            log.info("Task {} from job {} completed already! Skipping... ", task, jobName);
            return;
        }
        dataExporter.sendStagingData(companyJsonData, industryJsonData, tableName);
        this.setToCompleteStatus();
        log.info("Task {} from job {} completed!", task, jobName);
    }

}
