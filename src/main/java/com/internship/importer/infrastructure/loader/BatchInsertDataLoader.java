package com.internship.importer.infrastructure.loader;

import com.internship.importer.exception.JsonParsingException;
import com.internship.importer.infrastructure.format.StreamConverter;
import com.internship.importer.repository.StagingRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@RequiredArgsConstructor
public class BatchInsertDataLoader implements DataLoader {

    private final StagingRepository stagingRepository;
    private final StreamConverter converter;
    private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);


    public static int QUEUE_SIZE = 500;
    private static final int BATCH_SIZE = 250;
    private static final String POISON_PILL = "__EOF__";

    @Override
    public void loadData(InputStream inputStream, String tableName) {
        Thread producerThread = createProducerThread(inputStream);
        Thread consumerThread = createConsumerThread();

        producerThread.start();
        consumerThread.start();

        try {
            producerThread.join();
            queue.put(POISON_PILL);
            consumerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Data loading interrupted", e);
        }

    }

    private Thread createProducerThread(InputStream inputStream) {
        return new Thread(() -> {
            try {
                this.converter.convertInputStream(inputStream, queue);
            } catch (Exception e) {
                throw new JsonParsingException("Error in input stream conversion thread", e);
            }
        });
    }

    private Thread createConsumerThread() {
        return new Thread(() -> {
            try {
                List<String> batch = new ArrayList<>();
                while (true) {
                    String item = queue.take(); // blocking

                    if (POISON_PILL.equals(item)) {
                        break;
                    }

                    batch.add(item);

                    if (batch.size() >= BATCH_SIZE) {
                        stagingRepository.insertBatchRawJson(batch);
                        batch.clear();
                    }
                }

                if (!batch.isEmpty()) {
                    stagingRepository.insertBatchRawJson(batch);
                }

            } catch (Exception e) {
                throw new RuntimeException("Batch insert failed", e);
            }
        });
    }


}
