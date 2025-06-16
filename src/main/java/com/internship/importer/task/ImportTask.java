package com.internship.importer.task;

import com.internship.importer.data.DataLoader;
import com.internship.importer.data.DynamicDataSourceFactory;
import com.internship.importer.data.StagingTableManager;
import com.internship.importer.exception.DataFetchException;
import com.internship.importer.fetcher.DataFetcher;
import com.internship.importer.format.InputStreamConverter;
import com.internship.importer.handler.file.StatusFileHandler;
import com.internship.importer.handler.inputstream.InputStreamHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;


@Slf4j
public class ImportTask implements Task{

    private StatusFileHandler statusFileHandler;
    @Getter
    private String jobName;
    @Getter
    private String taskName = "import";
    private StagingTableManager tableManager;
    private String tableName;
    private DataFetcher dataFetcher;
    private DataLoader loader;
    private InputStreamHandler inputStreamHandler;

    public ImportTask(StatusFileHandler statusFileHandler, String jobName, StagingTableManager tableManager, String tableName, DataFetcher dataFetcher, DataLoader loader, InputStreamHandler inputStreamHandler) {
        this.statusFileHandler = statusFileHandler;
        this.jobName = jobName;
        this.tableManager = tableManager;
        this.tableName = tableName;
        this.dataFetcher = dataFetcher;
        this.loader = loader;
        this.inputStreamHandler = inputStreamHandler;
    }

    @Override
    public TaskStatus getStatus(){
        if (statusFileHandler.statusFileExists(jobName, taskName)){
            return TaskStatus.COMPLETE;
        }
        return TaskStatus.INCOMPLETE;
    }

    @Override
    public void setToIncompleteStatus(){
        statusFileHandler.deleteStatusFileIfExists(jobName, taskName);
    }

    @Override
    public void setToCompleteStatus(){
        statusFileHandler.createSuccessStatusFile(jobName, taskName);
    }

    @Override
    public void execute(){
        TaskStatus status = this.getStatus();
        if(status == TaskStatus.COMPLETE){
            log.info("Task {} from job {} completed already! Skipping... ", taskName, jobName);
            return;
        }
        tableManager.createStagingTable(tableName);
        try(InputStream inputStream = dataFetcher.fetchData()){
            inputStreamHandler.handle(inputStream, inputStream1 -> loader.loadData(inputStream1, tableName));
        }catch (IOException e) {
            throw new DataFetchException("Failed to fetch or process data for task " + taskName + " in job " + jobName, e);
        }
        this.setToCompleteStatus();
        log.info("Task {} from job {} completed!", taskName, jobName);

    }

}
