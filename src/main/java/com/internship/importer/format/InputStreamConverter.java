package com.internship.importer.format;

import java.io.InputStream;
import java.io.Writer;

public interface InputStreamConverter {
    void convertInputStream(InputStream inputStream, Writer writer);
}
