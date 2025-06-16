package com.internship.importer.handler.inputstream;

import java.io.InputStream;
import java.util.function.Consumer;

public class RawInputStreamHandler implements InputStreamHandler{
    @Override
    public void handle(InputStream inputStream, Consumer<InputStream> consumer) {
        consumer.accept(inputStream);
    }
}
