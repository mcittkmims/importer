package com.internship.importer.core.task;

import com.internship.importer.repository.RepositoryHelper;
import com.internship.importer.repository.StagingDataLoader;
import com.internship.importer.repository.StagingTableService;
import com.internship.importer.exception.DataFetchException;
import com.internship.importer.infrastructure.fetcher.DataFetcher;
import com.internship.importer.infrastructure.persistence.TaskStatusManager;
import com.internship.importer.infrastructure.compression.CompressionHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class DataImportTask implements Task {

    private TaskStatusManager taskStatusManager;
    @Getter
    private String jobName;
    @Getter
    private String taskName = "import";
    private StagingTableService tableService;
    private String tableName;
    private DataFetcher dataFetcher;
    private StagingDataLoader loader;
    private CompressionHandler compressionHandler;
    private RepositoryHelper repositoryHelper;

    public DataImportTask(TaskStatusManager taskStatusManager, String jobName, StagingTableService tableService,
            String tableName, DataFetcher dataFetcher, StagingDataLoader loader,
            CompressionHandler compressionHandler, RepositoryHelper repositoryHelper) {
        this.taskStatusManager = taskStatusManager;
        this.jobName = jobName;
        this.tableService = tableService;
        this.tableName = tableName;
        this.dataFetcher = dataFetcher;
        this.loader = loader;
        this.compressionHandler = compressionHandler;
        this.repositoryHelper = repositoryHelper;
    }

    @Override
    public TaskStatus getStatus() {
        if (taskStatusManager.statusFileExists(jobName, taskName)) {
            return TaskStatus.COMPLETE;
        }
        return TaskStatus.INCOMPLETE;
    }

    @Override
    public void setToIncompleteStatus() {
        taskStatusManager.deleteStatusFileIfExists(jobName, taskName);
    }

    @Override
    public void setToCompleteStatus() {
        taskStatusManager.createSuccessStatusFile(jobName, taskName);
    }

    @Override
    public void execute() {
        TaskStatus status = this.getStatus();
        if (status == TaskStatus.COMPLETE) {
            log.info("Task {} from job {} completed already! Skipping... ", taskName, jobName);
            return;
        }
        repositoryHelper.createTable(tableService);
        try (InputStream inputStream = dataFetcher.fetchData()) {
            compressionHandler.handle(inputStream, inputStream1 -> loader.loadData(inputStream1, tableName));
        } catch (IOException e) {
            throw new DataFetchException("Failed to fetch or process data for task " + taskName + " in job " + jobName,
                    e);
        }
        this.setToCompleteStatus();
        log.info("Task {} from job {} completed!", taskName, jobName);

    }

}
