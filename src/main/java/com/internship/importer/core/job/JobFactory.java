package com.internship.importer.core.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.importer.exception.JobCreationException;
import com.internship.importer.infrastructure.persistence.TaskStatusManager;
import com.internship.importer.domain.JobConfig;
import com.internship.importer.core.task.*;
import com.internship.importer.repository.DatabaseDataSourceFactory;
import lombok.AllArgsConstructor;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@AllArgsConstructor
public class JobFactory {

    private final DatabaseDataSourceFactory databaseDataSourceFactory;
    private final TaskFactory taskFactory;
    private final TaskStatusManager taskStatusManager;

    public Job createJob(String jobName, String json) {
        ObjectMapper mapper = new ObjectMapper();
        JobConfig config;
        try {
            config = mapper.readValue(json, JobConfig.class);
        } catch (JsonProcessingException e) {
            throw new JobCreationException("Failed to parse job config JSON for job: " + jobName, e);
        }

        DataSource dataSource = databaseDataSourceFactory.create(
                config.getDbConnection().getJdbc(),
                config.getDbCredentials().getUsername(),
                config.getDbCredentials().getPassword());

        DataImportTask dataImportTask = taskFactory.createImportTask(jobName, config, dataSource);
        DataExportTask dataExportTask = taskFactory.createExportTask(jobName, config, dataSource);

        TaskRetryStrategy retryStrategy = new DataImportRetryStrategy(432000, taskStatusManager, dataImportTask,
                dataExportTask);

        return new DataJob(retryStrategy, jobName, dataImportTask, dataExportTask);
    }
}
