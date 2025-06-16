package com.internship.importer.handler.inputstream;

import org.springframework.stereotype.Component;

@Component
public class InputStreamHandlerFactory {
    public InputStreamHandler getFromString(String type){
        return switch (type){
            case "zip" -> new ZipInputStreamHandler();
            case "none" -> new RawInputStreamHandler();
            default -> throw new IllegalArgumentException("Unsupported input stream type: " + type);
        };
    }
}
