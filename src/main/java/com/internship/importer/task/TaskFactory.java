package com.internship.importer.task;

import com.internship.importer.data.DataLoader;
import com.internship.importer.data.StagingDataAccessor;
import com.internship.importer.data.StagingTableManager;
import com.internship.importer.fetcher.DataFetcher;
import com.internship.importer.fetcher.DataFetcherFactory;
import com.internship.importer.format.InputStreamConverter;
import com.internship.importer.handler.file.StatusFileHandler;
import com.internship.importer.handler.inputstream.InputStreamHandler;
import com.internship.importer.handler.inputstream.InputStreamHandlerFactory;
import com.internship.importer.model.JobConfig;
import com.internship.importer.sender.DataSender;
import com.internship.importer.sender.HttpDataSender;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@AllArgsConstructor
public class TaskFactory {

    private final StatusFileHandler statusFileHandler;
    private final InputStreamHandlerFactory inputStreamHandlerFactory;
    private final InputStreamConverter inputStreamConverter;
    private final DataFetcherFactory dataFetcherFactory;

    public ImportTask createImportTask(
            String jobName,
            JobConfig config,
            DataSource dataSource
    ) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        StagingTableManager tableManager = new StagingTableManager(jdbcTemplate);
        DataLoader dataLoader = new DataLoader(dataSource, inputStreamConverter);
        InputStreamHandler handler = inputStreamHandlerFactory.getFromString(config.getArchived());
        DataFetcher dataFetcher = dataFetcherFactory.createDataFetcher(config.getSource());

        return new ImportTask(statusFileHandler, jobName, tableManager, config.getTable(), dataFetcher, dataLoader, handler);
    }

    public ExportTask createExportTask(
            String jobName,
            JobConfig config,
            DataSource dataSource
    ) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        StagingDataAccessor accessor = new StagingDataAccessor(jdbcTemplate);
        DataSender sender = new HttpDataSender(config.getExportUrl(), accessor);

        return new ExportTask(statusFileHandler, jobName, sender,
                config.getMappings().getCompany().toString(),
                config.getMappings().getIndustry().toString(),
                config.getTable());
    }
}

