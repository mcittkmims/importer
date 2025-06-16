package com.internship.importer.handler.inputstream;

import java.io.InputStream;
import java.util.function.Consumer;

public interface InputStreamHandler {
    void handle(InputStream inputStream, Consumer<InputStream> consumer);
}
