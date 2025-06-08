package com.internship.importer.exception;

public class FileResourceException extends RuntimeException {
    public FileResourceException(String message) {
        super(message);
    }
    
    public FileResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}