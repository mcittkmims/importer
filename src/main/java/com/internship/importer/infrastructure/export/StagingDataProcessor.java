package com.internship.importer.infrastructure.export;

import com.internship.importer.domain.JsonDataRecord;
import com.internship.importer.repository.StagingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StagingDataProcessor {

    @Transactional
    public void processBatch(StagingRepository repository, int batchSize, Consumer<List<JsonDataRecord>> processor) {
        System.out.println("hey");
        List<JsonDataRecord> records = repository.getBatchJsonData(batchSize);
        System.out.println("hey");

        log.info("Processing batch with {} records", records.size());

        processor.accept(records);

        List<Long> ids = records.stream()
                .map(JsonDataRecord::getId)
                .collect(Collectors.toList());

        int updatedRows = repository.markRowsByIds(ids);
        log.info("Marked {} records as exported", updatedRows);
    }
}
