package com.internship.importer.core.task;

import com.internship.importer.infrastructure.format.StreamConverterFactory;
import com.internship.importer.repository.StagingDataLoader;
import com.internship.importer.repository.StagingTableService;
import com.internship.importer.repository.PartitionRepository;
import com.internship.importer.infrastructure.fetcher.DataFetcher;
import com.internship.importer.infrastructure.fetcher.DataFetcherFactory;
import com.internship.importer.infrastructure.persistence.TaskStatusManager;
import com.internship.importer.infrastructure.compression.CompressionHandler;
import com.internship.importer.infrastructure.compression.CompressionHandlerFactory;
import com.internship.importer.domain.JobConfig;
import com.internship.importer.infrastructure.export.DataExporter;
import com.internship.importer.infrastructure.export.HttpDataExporter;
import com.internship.importer.infrastructure.export.PartitionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
public class TaskFactory {

        private final TaskStatusManager taskStatusManager;
        private final CompressionHandlerFactory compressionHandlerFactory;
        private final StreamConverterFactory streamConverterFactory;
        private final DataFetcherFactory dataFetcherFactory;
        @Qualifier("exportExecutorService")
        private final ExecutorService exportExecutorService;

        public TaskFactory(TaskStatusManager taskStatusManager, CompressionHandlerFactory compressionHandlerFactory,
                        StreamConverterFactory streamConverterFactory, DataFetcherFactory dataFetcherFactory,
                        ExecutorService exportExecutorService) {
                this.taskStatusManager = taskStatusManager;
                this.compressionHandlerFactory = compressionHandlerFactory;
                this.streamConverterFactory = streamConverterFactory;
                this.dataFetcherFactory = dataFetcherFactory;
                this.exportExecutorService = exportExecutorService;
        }

        public DataImportTask createImportTask(
                        String jobName,
                        JobConfig config,
                        javax.sql.DataSource dataSource) {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

                StagingDataLoader dataLoader = new StagingDataLoader(dataSource,
                                streamConverterFactory.getConverter(config.getDataType()));
                StagingTableService tableService = new StagingTableService(jdbcTemplate, config.getTable());

                CompressionHandler handler = compressionHandlerFactory.getFromString(config.getArchived());
                DataFetcher dataFetcher = dataFetcherFactory.createDataFetcher(config.getSource());

                return new DataImportTask(taskStatusManager, jobName, tableService, config.getTable(), dataFetcher,
                                dataLoader,
                                handler);
        }

        public DataExportTask createExportTask(
                        String jobName,
                        JobConfig config,
                        javax.sql.DataSource dataSource) {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                PartitionRepository partitionRepository = new PartitionRepository(jdbcTemplate, config.getTable());

                PartitionManager partitionManager = new PartitionManager(partitionRepository);

                StagingTableService tableService = new StagingTableService(jdbcTemplate, config.getTable());
                tableService.createPartitionTable();

                tableService.createStagingTable();;

                DataExporter exporter = new HttpDataExporter(
                                config.getExportUrl(),
                                partitionRepository,
                                exportExecutorService,
                                partitionManager);

                return new DataExportTask(taskStatusManager, jobName, exporter,
                                config.getMappings().getCompany().toString(),
                                config.getMappings().getIndustry().toString(),
                                config.getMappings().getTaxAuthority().toString(),
                                config.getMappings().getTaxInfo().toString());
        }
}
