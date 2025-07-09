package com.internship.importer.infrastructure.export;

import com.internship.importer.repository.PartitionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PartitionManager {

    private final PartitionRepository partitionRepository;
    private static final int DEFAULT_BATCH_SIZE = 100;

//    public int addPartitionsForNewData(int batchSize) {
//        try {
//            int partitionsCreated = partitionRepository.createPartitionsForNewData(batchSize);
//            return partitionsCreated;
//        } catch (Exception e) {
//            throw new RuntimeException("Partition creation failed", e);
//        }
//    }
//
//
//    public int addPartitionsForNewData() {
//        return addPartitionsForNewData(DEFAULT_BATCH_SIZE);
//    }
//
//
//    public String getPartitionInfo() {
//        boolean hasUnprocessed = partitionRepository.hasUnprocessedBatches();
//        int pending = partitionRepository.countPendingBatches();
//        int processing = partitionRepository.countProcessingBatches();
//        int failed = partitionRepository.countFailedBatches();
//
//        return String.format("Partition status - Pending: %d, Processing: %d, Failed: %d, Has unprocessed: %s",
//                           pending, processing, failed, hasUnprocessed);
//    }

}
