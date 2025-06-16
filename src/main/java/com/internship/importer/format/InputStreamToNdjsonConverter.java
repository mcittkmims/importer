package com.internship.importer.format;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.importer.exception.JsonParsingException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

@Component
public class InputStreamToNdjsonConverter implements InputStreamConverter{

    public void convertInputStream(InputStream inputStream, Writer writer) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getFactory();
            JsonParser parser = factory.createParser(inputStream);

            JsonToken token = parser.nextToken();

            switch (token) {
                case START_ARRAY:
                    writeArrayAsNdjson(parser, mapper, writer);
                    break;

                case START_OBJECT:
                    writeNdjsonStream(parser, mapper, writer);
                    break;

                default:
                    throw new JsonParsingException("Unsupported JSON format: expected array or NDJSON, found: " + token);
            }

            writer.flush();
        } catch (IOException e) {
            throw new JsonParsingException("Failed to parse JSON data", e);
        }
    }

    private void writeArrayAsNdjson(JsonParser parser, ObjectMapper mapper, Writer writer) {
        try {
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                JsonNode node = mapper.readTree(parser);

                String jsonLine = mapper.writeValueAsString(node).replace("＼", "\\uFF3C").replace("\\", "\\\\");
                writer.write(jsonLine);
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new JsonParsingException("Failed to write JSON array as NDJSON", e);
        }
    }

    private void writeNdjsonStream(JsonParser parser, ObjectMapper mapper, Writer writer) {
        try {
            JsonNode node = mapper.readTree(parser);

            writer.write(mapper.writeValueAsString(node).replace("＼", "\\uFF3C").replace("\\", "\\\\"));
            writer.write('\n');

            while (parser.nextToken() == JsonToken.START_OBJECT) {
                node = mapper.readTree(parser);
                writer.write(mapper.writeValueAsString(node).replace("＼", "\\uFF3C").replace("\\", "\\\\"));
                writer.write('\n');
            }
        } catch (IOException e) {
            throw new JsonParsingException("Failed to write NDJSON stream", e);
        }
    }
}
