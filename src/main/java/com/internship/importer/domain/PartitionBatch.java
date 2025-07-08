package com.internship.importer.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PartitionBatch {
    private final Long id;
    private final Integer startId;
    private final Integer endId;
    private final Boolean status;
    private final ProcessingStatus processingStatus;
}
