package com.internship.importer.handler.inputstream;

import com.internship.importer.exception.ZipExtractionException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipInputStreamHandler implements InputStreamHandler {

    @Override
    public void handle(InputStream inputStream, Consumer<InputStream> consumer) {
        try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(inputStream))) {
            extractZipEntries(zipIn, consumer);
        } catch (IOException e) {
            throw new ZipExtractionException("Failed to handle the zip input stream" + e);
        }
    }

    private void extractZipEntries(ZipInputStream zipIn, Consumer<InputStream> consumer) {
        try {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    consumer.accept(zipIn);
                }
                zipIn.closeEntry();
            }
        } catch (IOException e) {
            throw new ZipExtractionException("Failed to extract ZIP entries", e);
        }
    }
}
