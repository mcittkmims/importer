package com.internship.importer.loader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.importer.exception.DataCopyException;
import com.internship.importer.exception.JsonParsingException;
import lombok.AllArgsConstructor;
import org.postgresql.PGConnection;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@AllArgsConstructor
public class JsonDataLoader implements DataLoader {

    private final DataSource dataSource;

    public void loadData(InputStream inputStream) {
        try (Connection connection = dataSource.getConnection()) {
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);

            Thread writerThread = createWriterThread(inputStream, out);
            writerThread.start();

            copyToDatabase(connection, in);
        } catch (SQLException e) {
            throw new DataCopyException("Failed to establish database connection for data loading", e);
        } catch (IOException e) {
            throw new DataCopyException("Failed to load data into database", e);
        }
    }

    private void copyToDatabase(Connection connection, InputStream in) {
        try (in; Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE company_staging");

            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            pgConnection.getCopyAPI().copyIn("COPY company_staging (raw_json) FROM STDIN", in);

        } catch (SQLException e) {
            throw new DataCopyException("Error during database COPY operation", e);
        } catch (IOException e) {
            throw new DataCopyException("Error during data stream processing", e);
        }
    }

    private Thread createWriterThread(InputStream inputStream, OutputStream out) {
        return new Thread(() -> {
            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                this.convertToNdjsonStream(inputStream, writer);
            } catch (IOException e) {
                throw new JsonParsingException("Error in JSON conversion thread", e);
            }
        });
    }

    private void convertToNdjsonStream(InputStream inputStream, Writer writer) {
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

