package com.internship.importer.infrastructure.export;

import com.internship.importer.domain.PartitionBatch;
import com.internship.importer.repository.StagingRepository;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class PartitionDataExporter implements DataExporter {
    private final StagingRepository stagingRepository;
    private final String postUrl;
    private final ExecutorService executor;
    public static int PARTITION_LIMIT = 100;

    public void sendStagingData(String companyJsonData, String industryJsonData, String taxAuthorityJsonData, String taxInfoJsonData) {
        List<PartitionBatch> partitions = stagingRepository.lockAndFetchPartitions(PARTITION_LIMIT);
        for (PartitionBatch partition : partitions) {
            executor.submit(new PartitionProcessor(partition, stagingRepository, postUrl, companyJsonData, industryJsonData, taxAuthorityJsonData,taxInfoJsonData));
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                System.err.println("Timeout while waiting for tasks to complete.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


