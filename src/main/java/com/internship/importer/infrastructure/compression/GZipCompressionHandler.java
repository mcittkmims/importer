package com.internship.importer.infrastructure.compression;

import com.internship.importer.exception.ZipExtractionException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GZipCompressionHandler implements CompressionHandler{
    @Override
    public void handle(InputStream inputStream, Consumer<InputStream> consumer) {
        try (GZIPInputStream zipIn = new GZIPInputStream(new BufferedInputStream(inputStream))) {
            consumer.accept(zipIn);
        } catch (IOException e) {
            throw new ZipExtractionException("Failed to handle the zip input stream" + e);
        }
    }
}
