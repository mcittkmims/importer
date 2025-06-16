package com.internship.importer.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.importer.data.DynamicDataSourceFactory;
import com.internship.importer.exception.JobCreationException;
import com.internship.importer.fetcher.DataFetcherFactory;
import com.internship.importer.handler.file.StatusFileHandler;
import com.internship.importer.model.JobConfig;
import com.internship.importer.task.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@AllArgsConstructor
public class JobFactory {

    private final DynamicDataSourceFactory dynamicDataSourceFactory;
    private final TaskFactory taskFactory;
    private final StatusFileHandler statusFileHandler;

    public Job createJob(String jobName, String json){
        ObjectMapper mapper = new ObjectMapper();
        JobConfig config;
        try {
            config = mapper.readValue(json, JobConfig.class);
        } catch (JsonProcessingException e) {
            throw new JobCreationException("Failed to parse job config JSON for job: " + jobName, e);
        }

        DataSource dataSource = dynamicDataSourceFactory.create(
                config.getDbConnection().getJdbc(),
                config.getDbCredentials().getUsername(),
                config.getDbCredentials().getPassword()
        );

        ImportTask importTask = taskFactory.createImportTask(jobName, config, dataSource);
        ExportTask exportTask = taskFactory.createExportTask(jobName, config, dataSource);

        TaskRetryEvaluator retryEvaluator = new ImportTaskRetryEvaluator(8640000,statusFileHandler, importTask, exportTask);

        return new DataJob(retryEvaluator,jobName, importTask, exportTask);
    }
}
