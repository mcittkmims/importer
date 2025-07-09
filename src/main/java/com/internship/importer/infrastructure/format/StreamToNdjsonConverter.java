package com.internship.importer.infrastructure.format;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.importer.exception.JsonParsingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;

@Component
@Slf4j
public class StreamToNdjsonConverter implements StreamConverter {

    public void convertInputStream(InputStream inputStream, Writer writer) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getFactory();
            JsonParser parser = factory.createParser(inputStream);

            log.info("JSON stream converter started...");

            JsonToken token = parser.nextToken();

            switch (token) {
                case START_ARRAY:
                    writeArrayAsNdjsonToWriter(parser, mapper, writer);
                    break;

                case START_OBJECT:
                    writeNdjsonStreamToWriter(parser, mapper, writer);
                    break;

                default:
                    throw new JsonParsingException("Unsupported JSON format: expected array or NDJSON, found: " + token);
            }

            writer.flush();
        } catch (IOException e) {
            throw new JsonParsingException("Failed to parse JSON data", e);
        }
    }

    private void writeArrayAsNdjsonToWriter(JsonParser parser, ObjectMapper mapper, Writer writer) {
        try {
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                JsonNode node = mapper.readTree(parser);
                String jsonLine = mapper.writeValueAsString(node)
                        .replace("＼", "\\uFF3C")
                        .replace("\\", "\\\\");
                writer.write(jsonLine);
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new JsonParsingException("Failed to write JSON array as NDJSON", e);
        }
    }

    private void writeNdjsonStreamToWriter(JsonParser parser, ObjectMapper mapper, Writer writer) {
        try {
            JsonNode node = mapper.readTree(parser);
            writer.write(mapper.writeValueAsString(node)
                    .replace("＼", "\\uFF3C")
                    .replace("\\", "\\\\"));
            writer.write('\n');

            while (parser.nextToken() == JsonToken.START_OBJECT) {
                node = mapper.readTree(parser);
                writer.write(mapper.writeValueAsString(node)
                        .replace("＼", "\\uFF3C")
                        .replace("\\", "\\\\"));
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new JsonParsingException("Failed to write NDJSON stream", e);
        }
    }

    public void convertInputStream(InputStream inputStream, BlockingQueue<String> queue) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getFactory();
            JsonParser parser = factory.createParser(inputStream);

            log.info("JSON stream queue converter started...");

            JsonToken token = parser.nextToken();

            switch (token) {
                case START_ARRAY:
                    writeArrayAsNdjsonToQueue(parser, mapper, queue);
                    break;

                case START_OBJECT:
                    writeNdjsonStreamToQueue(parser, mapper, queue);
                    break;

                default:
                    throw new JsonParsingException("Unsupported JSON format: expected array or NDJSON, found: " + token);
            }

        } catch (IOException | InterruptedException e) {
            throw new JsonParsingException("Failed to parse JSON data to queue", e);
        }
    }



    private void writeArrayAsNdjsonToQueue(JsonParser parser, ObjectMapper mapper, BlockingQueue<String> queue) throws IOException, InterruptedException {
        while (parser.nextToken() == JsonToken.START_OBJECT) {
            JsonNode node = mapper.readTree(parser);
            String jsonLine = mapper.writeValueAsString(node);
            queue.put(jsonLine);  // blocks if full
        }
    }

    private void writeNdjsonStreamToQueue(JsonParser parser, ObjectMapper mapper, BlockingQueue<String> queue) throws IOException, InterruptedException {
        JsonNode node = mapper.readTree(parser);
        queue.put(mapper.writeValueAsString(node));

        while (parser.nextToken() == JsonToken.START_OBJECT) {
            node = mapper.readTree(parser);
            queue.put(mapper.writeValueAsString(node));
        }
    }
}
