package com.internship.importer.exception;

public class ZipExtractionException extends RuntimeException {
    public ZipExtractionException(String message) {
        super(message);
    }
    
    public ZipExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}