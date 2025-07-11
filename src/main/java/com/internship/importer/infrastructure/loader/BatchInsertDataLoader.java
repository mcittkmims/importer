package com.internship.importer.infrastructure.loader;

import com.internship.importer.domain.JsonData;
import com.internship.importer.domain.PartitionBatch;
import com.internship.importer.exception.JsonParsingException;
import com.internship.importer.infrastructure.format.StreamConverter;
import com.internship.importer.repository.StagingRepository;
import com.internship.importer.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@RequiredArgsConstructor
public class BatchInsertDataLoader implements DataLoader {

    private final StagingRepository stagingRepository;
    private final StreamConverter converter;

    private LinkedBlockingQueue<String> jsonQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private LinkedBlockingQueue<Long> idQueue = new LinkedBlockingQueue<>(ID_QUEUE_SIZE);

    public static int QUEUE_SIZE = 500;
    public static int ID_QUEUE_SIZE = 2000;
    private static final int BATCH_SIZE = 250;
    private static final int ID_BATCH_SIZE = 250;
    private static final int PARTITION_BATCH_SIZE = 10;
    private static final String POISON_PILL = "__EOF__";
    private static final Long ID_POISON_PILL = -1L;

    @Override
    public void loadData(InputStream inputStream, String tableName) {
        Thread producerThread = createProducerThread(inputStream);
        Thread consumerThread = createConsumerThread();
        Thread partitionThread = createPartitionConsumerThread();

        producerThread.start();
        consumerThread.start();
        partitionThread.start();

        try {
            producerThread.join();
            jsonQueue.put(POISON_PILL);
            consumerThread.join();
            idQueue.put(ID_POISON_PILL);
            partitionThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Data loading interrupted", e);
        }
    }

    private Thread createProducerThread(InputStream inputStream) {
        return new Thread(() -> {
            try {
                this.converter.convertInputStream(inputStream, jsonQueue);
            } catch (Exception e) {
                throw new JsonParsingException("Error in input stream conversion thread", e);
            }
        });
    }

    private Thread createConsumerThread() {
        return new Thread(() -> {
            try {
                List<JsonData> batch = new ArrayList<>();
                while (true) {
                    String item = jsonQueue.take(); // blocking

                    if (POISON_PILL.equals(item)) {
                        break;
                    }

                    batch.add(new JsonData(item, HashUtil.sha256Hex(item)));

                    if (batch.size() >= BATCH_SIZE) {
                        List<Long> newIds = stagingRepository.insertBatchJsonDataReturnIds(batch);
                        for (Long id : newIds) {
                            idQueue.put(id);
                        }
                        batch.clear();
                    }
                }

                if (!batch.isEmpty()) {
                    List<Long> newIds = stagingRepository.insertBatchJsonDataReturnIds(batch);
                    for (Long id : newIds) {
                        idQueue.put(id);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Batch insert failed", e);
            }
        });
    }

    private Thread createPartitionConsumerThread() {
        return new Thread(() -> {
            try {
                List<Long> idBatch = new ArrayList<>();
                List<PartitionBatch> partitionBatches = new ArrayList<>();
                
                while (true) {
                    Long id = idQueue.take(); // blocking
                    
                    if (ID_POISON_PILL.equals(id)) {
                        break;
                    }
                    
                    idBatch.add(id);
                    
                    if (idBatch.size() >= ID_BATCH_SIZE) {
                        PartitionBatch partitionBatch = createPartitionBatch(idBatch);
                        partitionBatches.add(partitionBatch);
                        idBatch.clear();
                        
                        if (partitionBatches.size() >= PARTITION_BATCH_SIZE) {
                            stagingRepository.insertPartitionBatches(partitionBatches);
                            partitionBatches.clear();
                        }
                    }
                }
                
                if (!idBatch.isEmpty()) {
                    partitionBatches.add(createPartitionBatch(idBatch));
                }
                
                if (!partitionBatches.isEmpty()) {
                    stagingRepository.insertPartitionBatches(partitionBatches);
                }
                
            } catch (Exception e) {
                throw new RuntimeException("Partition creation failed", e);
            }
        });
    }
    
    private PartitionBatch createPartitionBatch(List<Long> ids) {
        Collections.sort(ids);
        Integer minId = ids.get(0).intValue();
        Integer maxId = ids.get(ids.size() - 1).intValue();
        return new PartitionBatch(minId, maxId);
    }
}
