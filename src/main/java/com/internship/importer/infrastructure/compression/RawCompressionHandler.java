package com.internship.importer.infrastructure.compression;

import java.io.InputStream;
import java.util.function.Consumer;

public class RawCompressionHandler implements CompressionHandler{
    @Override
    public void handle(InputStream inputStream, Consumer<InputStream> consumer) {
        consumer.accept(inputStream);
    }
}
