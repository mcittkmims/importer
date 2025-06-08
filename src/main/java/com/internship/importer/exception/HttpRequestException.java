package com.internship.importer.exception;

public class HttpRequestException extends RuntimeException {
    private final int statusCode;
    
    public HttpRequestException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public HttpRequestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}