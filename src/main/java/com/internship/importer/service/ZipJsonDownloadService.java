package com.internship.importer.service;

import com.internship.importer.config.ImportAppConfig;
import com.internship.importer.exception.UrlConnectionException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@AllArgsConstructor
public class ZipJsonDownloadService implements JsonDownloadService {

    private ImportAppConfig config;

    @Override
    public void downloadFile(String urlString, Consumer<InputStream> consumer){
        HttpURLConnection connection = createConnection(urlString);
        try (InputStream inputStream = connection.getInputStream();
             ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(inputStream))) {

            extractZipEntries(zipIn, consumer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process the downloaded content: " + e);
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection createConnection(String urlString){
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setRequestMethod(config.getHttpMethod());
            connection.setDoOutput(true);
            connection.connect();
            return connection;
        } catch (IOException e) {
            throw new UrlConnectionException("Failed to connect to URL: " + urlString, e);
        }
    }

    private void extractZipEntries(ZipInputStream zipIn, Consumer<InputStream> consumer) throws IOException {
        ZipEntry entry;
        while ((entry = zipIn.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
               consumer.accept(zipIn);
            }
            zipIn.closeEntry();
        }
    }


}

