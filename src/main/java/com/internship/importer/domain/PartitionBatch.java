package com.internship.importer.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class PartitionBatch {
    private Long id;
    private final Integer startId;
    private final Integer endId;
    private Boolean sentStatus;
}
