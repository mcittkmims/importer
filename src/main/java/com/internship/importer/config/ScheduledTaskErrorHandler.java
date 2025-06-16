package com.internship.importer.config;

import com.internship.importer.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import java.sql.SQLException;

@Component
public class ScheduledTaskErrorHandler implements ErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskErrorHandler.class);
    
    @Override
    public void handleError(Throwable t) {
        if (t instanceof DataImportException) {
            handleDataImportError((DataImportException) t);
        } else if (t instanceof DataExportException) {
            handleDataExportError((DataExportException) t);
        } else if (t instanceof DatabaseException) {
            handleDatabaseError((DatabaseException) t);
        } else if (t instanceof UrlConnectionException) {
            handleUrlConnectionError((UrlConnectionException) t);
        } else if (t instanceof JsonParsingException) {
            handleJsonParsingError((JsonParsingException) t);
        } else if (t instanceof ZipExtractionException) {
            handleZipExtractionError((ZipExtractionException) t);
        } else if (t instanceof FileResourceException) {
            handleFileResourceError((FileResourceException) t);
        } else if (t instanceof HttpRequestException) {
            handleHttpRequestError((HttpRequestException) t);
        } else if (t instanceof DataCopyException) {
            handleDataCopyError((DataCopyException) t);
        } else if (t instanceof DataFetchException) {
            handleDataFetchError((DataFetchException) t);
        } else if (t instanceof FolderCreationException) {
            handleFolderCreationError((FolderCreationException) t);
        } else if (t instanceof InvalidJobConfigFileException) {
            handleInvalidJobConfigFileError((InvalidJobConfigFileException) t);
        } else if (t instanceof JobConfigException) {
            handleJobConfigError((JobConfigException) t);
        } else if (t instanceof JobCreationException) {
            handleJobCreationError((JobCreationException) t);
        } else if (t instanceof StatusFileException) {
            handleStatusFileError((StatusFileException) t);
        } else if (t instanceof SQLException) {
            handleSqlError((SQLException) t);
        } else if (t instanceof RuntimeException) {
            handleRuntimeError((RuntimeException) t);
        } else {
            handleGenericError(t);
        }
    }
    
    private void handleDataImportError(DataImportException e) {
        logger.error("Data Import Failed: {}", e.getMessage(), e);
    }
    
    private void handleDataExportError(DataExportException e) {
        logger.error("Data Export Failed: {}", e.getMessage(), e);
    }
    
    private void handleDatabaseError(DatabaseException e) {
        logger.error("Database Operation Failed: {}", e.getMessage(), e);
    }
    
    private void handleUrlConnectionError(UrlConnectionException e) {
        logger.error("Network Connection Failed: {}", e.getMessage(), e);
    }
    
    private void handleJsonParsingError(JsonParsingException e) {
        logger.error("JSON Parsing Failed: {}", e.getMessage(), e);
    }
    
    private void handleZipExtractionError(ZipExtractionException e) {
        logger.error("ZIP Extraction Failed: {}", e.getMessage(), e);
    }
    
    private void handleFileResourceError(FileResourceException e) {
        logger.error("File Resource Error: {}", e.getMessage(), e);
    }
    
    private void handleHttpRequestError(HttpRequestException e) {
        if (e.getStatusCode() > 0) {
            logger.error("HTTP Request Failed - Status: {}, Message: {}", e.getStatusCode(), e.getMessage(), e);
        } else {
            logger.error("HTTP Request Failed: {}", e.getMessage(), e);
        }
    }
    
    private void handleDataCopyError(DataCopyException e) {
        logger.error("Data Copy Operation Failed: {}", e.getMessage(), e);
    }
    
    private void handleDataFetchError(DataFetchException e) {
        logger.error("Data Fetch Failed: {}", e.getMessage(), e);
    }
    
    private void handleFolderCreationError(FolderCreationException e) {
        logger.error("Folder Creation Failed: {}", e.getMessage(), e);
    }
    
    private void handleInvalidJobConfigFileError(InvalidJobConfigFileException e) {
        logger.error("Invalid Job Config File: {}", e.getMessage(), e);
    }
    
    private void handleJobConfigError(JobConfigException e) {
        logger.error("Job Configuration Error: {}", e.getMessage(), e);
    }
    
    private void handleJobCreationError(JobCreationException e) {
        logger.error("Job Creation Failed: {}", e.getMessage(), e);
    }
    
    private void handleStatusFileError(StatusFileException e) {
        logger.error("Status File Operation Failed: {}", e.getMessage(), e);
    }
    
    private void handleSqlError(SQLException e) {
        logger.error("SQL Error - Code: {}, State: {}, Message: {}",
                    e.getErrorCode(), e.getSQLState(), e.getMessage(), e);
    }
    
    private void handleRuntimeError(RuntimeException e) {
        logger.error("Runtime Exception in scheduled task: {}", e.getMessage(), e);
    }
    
    private void handleGenericError(Throwable t) {
        logger.error("Unexpected error in scheduled task: {}", t.getMessage(), t);
    }
}