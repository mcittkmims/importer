package com.internship.importer.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class IteratorInputStream extends InputStream {

    private final Iterator<String> iterator;
    private byte[] currentBytes = null;
    private int currentIndex = 0;

    public IteratorInputStream(Iterator<String> iterator) {
        this.iterator = iterator;
    }

    @Override
    public int read() throws IOException {
        if (currentBytes == null || currentIndex >= currentBytes.length) {
            if (!iterator.hasNext()) {
                return -1; // End of stream
            }
            String nextString = iterator.next() + "\n"; // Add newline if needed
            currentBytes = nextString.getBytes(StandardCharsets.UTF_8);
            currentIndex = 0;
        }
        return currentBytes[currentIndex++] & 0xFF;
    }
}
