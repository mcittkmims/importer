package com.internship.importer.infrastructure.format;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StreamConverterFactory {
    private final XmlToNdjsonConverter xmlToNdjsonConverter;
    private final StreamToNdjsonConverter streamToNdjsonConverter;
    public StreamConverter getConverter(String type){
        return switch(type){
            case "xml" -> xmlToNdjsonConverter;
            case "zip" -> streamToNdjsonConverter;
            default -> throw new IllegalArgumentException("Non existent type");
        };
    }
}
