package com.internship.importer.exception;

public class DataCopyException extends RuntimeException {
    public DataCopyException(String message) {
        super(message);
    }
    
    public DataCopyException(String message, Throwable cause) {
        super(message, cause);
    }
}