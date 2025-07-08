package com.internship.importer.domain;

public enum ProcessingStatus {
    PENDING,     // Ready to be processed
    PROCESSING,  // Currently being processed by a thread
    COMPLETED,   // Successfully processed
    FAILED       // Processing failed, can be retried
}
