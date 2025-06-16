package com.internship.importer.task;

import com.internship.importer.handler.file.StatusFileHandler;
import com.internship.importer.sender.DataSender;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExportTask implements Task{

    private StatusFileHandler statusFileHandler;
    private String jobName;
    private String task = "export";
    private DataSender dataSender;
    private String companyJsonData;
    private String industryJsonData;
    private String tableName;

    public ExportTask(StatusFileHandler statusFileHandler, String jobName, DataSender dataSender, String companyJsonData, String industryJsonData, String tableName) {
        this.statusFileHandler = statusFileHandler;
        this.jobName = jobName;
        this.dataSender = dataSender;
        this.companyJsonData = companyJsonData;
        this.industryJsonData = industryJsonData;
        this.tableName = tableName;
    }

    @Override
    public TaskStatus getStatus(){
        if (statusFileHandler.statusFileExists(jobName, task)){
            return TaskStatus.COMPLETE;
        }
        return TaskStatus.INCOMPLETE;
    }

    @Override
    public void setToIncompleteStatus(){
        statusFileHandler.deleteStatusFileIfExists(jobName, task);
    }

    @Override
    public void setToCompleteStatus(){
        statusFileHandler.createSuccessStatusFile(jobName, task);
    }

    @Override
    public void execute(){
        TaskStatus status = this.getStatus();
        if(status == TaskStatus.COMPLETE){
            log.info("Task {} from job {} completed already! Skipping... ", task, jobName);
            return;
        }
        dataSender.sendStagingData(companyJsonData,industryJsonData,tableName);
        this.setToCompleteStatus();
        log.info("Task {} from job {} completed!", task, jobName);
    }

}
