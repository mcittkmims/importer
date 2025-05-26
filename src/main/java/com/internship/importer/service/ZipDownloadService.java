package com.internship.importer.service;

import com.internship.importer.config.ImportAppConfig;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@AllArgsConstructor
public class ZipDownloadService implements DownloadService {

    private ImportAppConfig config;

    @Override
    public void downloadFile(String urlString, Path targetFolder) throws IOException {
        Files.createDirectories(targetFolder);

        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod(config.getHttpMethod());
        connection.setDoOutput(true);
        connection.connect();

        try (InputStream inputStream = connection.getInputStream();
             ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(inputStream))) {

            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                Path filePath = targetFolder.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());

                    try (OutputStream out = Files.newOutputStream(filePath)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zipIn.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipIn.closeEntry();
            }
        } finally {
            connection.disconnect();
        }
    }

}

