package com.internship.importer.infrastructure.compression;

import org.springframework.stereotype.Component;

@Component
public class CompressionHandlerFactory {
    public CompressionHandler getFromString(String type){
        return switch (type){
            case "zip" -> new ZipCompressionHandler();
            case "gzip" -> new GZipCompressionHandler();
            case "xml" -> new RawCompressionHandler();
            default -> throw new IllegalArgumentException("Unsupported compression type: " + type);
        };
    }
}
