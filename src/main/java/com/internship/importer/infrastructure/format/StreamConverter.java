package com.internship.importer.infrastructure.format;

import java.io.InputStream;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;

public interface StreamConverter {
    void convertInputStream(InputStream inputStream, Writer writer);

    void convertInputStream(InputStream inputStream, BlockingQueue<String> queue);

}
