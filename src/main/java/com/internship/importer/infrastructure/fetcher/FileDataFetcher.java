package com.internship.importer.infrastructure.fetcher;

import com.internship.importer.exception.FileAccessException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FileDataFetcher implements DataFetcher {

    private String filePath;

    @Override
    public InputStream fetchData() {
        try {
            return new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            throw new FileAccessException("Failed to access file: " + filePath, e);
        }
    }
}