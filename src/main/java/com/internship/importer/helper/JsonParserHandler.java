package com.internship.importer.helper;

import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

@FunctionalInterface
public interface JsonParserHandler {
    void handle(JsonParser parser) throws IOException;
}
