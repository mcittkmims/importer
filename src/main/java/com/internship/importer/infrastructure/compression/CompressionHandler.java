package com.internship.importer.infrastructure.compression;

import java.io.InputStream;
import java.util.function.Consumer;

public interface CompressionHandler {
    void handle(InputStream inputStream, Consumer<InputStream> consumer);
}
