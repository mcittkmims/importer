package com.internship.importer.core.task;

import com.internship.importer.repository.StagingDataLoader;
import com.internship.importer.repository.StagingRepository;
import com.internship.importer.repository.StagingTableService;
import com.internship.importer.infrastructure.fetcher.DataFetcher;
import com.internship.importer.infrastructure.fetcher.DataFetcherFactory;
import com.internship.importer.infrastructure.format.StreamConverter;
import com.internship.importer.infrastructure.persistence.TaskStatusManager;
import com.internship.importer.infrastructure.compression.CompressionHandler;
import com.internship.importer.infrastructure.compression.CompressionHandlerFactory;
import com.internship.importer.domain.JobConfig;
import com.internship.importer.infrastructure.export.DataExporter;
import com.internship.importer.infrastructure.export.HttpDataExporter;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TaskFactory {

    private final TaskStatusManager taskStatusManager;
    private final CompressionHandlerFactory compressionHandlerFactory;
    private final StreamConverter streamConverter;
    private final DataFetcherFactory dataFetcherFactory;

    public DataImportTask createImportTask(
            String jobName,
            JobConfig config,
            javax.sql.DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        StagingTableService tableService = new StagingTableService(jdbcTemplate);
        StagingDataLoader dataLoader = new StagingDataLoader(dataSource, streamConverter);
        CompressionHandler handler = compressionHandlerFactory.getFromString(config.getArchived());
        DataFetcher dataFetcher = dataFetcherFactory.createDataFetcher(config.getSource());

        return new DataImportTask(taskStatusManager, jobName, tableService, config.getTable(), dataFetcher, dataLoader,
                handler);
    }

    public DataExportTask createExportTask(
            String jobName,
            JobConfig config,
            javax.sql.DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        StagingRepository repository = new StagingRepository(jdbcTemplate);
        DataExporter exporter = new HttpDataExporter(config.getExportUrl(), repository);

        return new DataExportTask(taskStatusManager, jobName, exporter,
                config.getMappings().getCompany().toString(),
                config.getMappings().getIndustry().toString(),
                config.getTable());
    }
}
