package com.internship.importer.infrastructure.format;

import java.io.InputStream;
import java.io.Writer;

public interface StreamConverter {
    void convertInputStream(InputStream inputStream, Writer writer);
}
