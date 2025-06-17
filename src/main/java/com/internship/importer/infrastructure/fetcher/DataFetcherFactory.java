package com.internship.importer.infrastructure.fetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.internship.importer.domain.HttpSource;
import org.springframework.stereotype.Component;

@Component
public class DataFetcherFactory {
    public DataFetcher createDataFetcher(JsonNode json) {
        String type = json.get("type").asText();
        return switch (type.toLowerCase()) {
            case "http" -> {
                HttpSource source = new HttpSource();
                source.setUrl(json.get("url").asText());
                source.setMethod(json.get("method").asText());
                yield new HttpDataFetcher(source);
            }
            default -> throw new IllegalArgumentException("Unsupported source type: " + type);
        };
    }
}
