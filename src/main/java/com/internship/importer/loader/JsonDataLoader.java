package com.internship.importer.loader;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Failed to load data into database", e);
        }
    }


    private void copyToDatabase(Connection connection, InputStream in) {
        try (in; Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE company_staging");

            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            pgConnection.getCopyAPI().copyIn("COPY company_staging (raw_json) FROM STDIN", in);

        } catch (SQLException | IOException e) {
            throw new RuntimeException("Error during COPY operation", e);
        }
    }

    private Thread createWriterThread(InputStream inputStream, OutputStream out) {
        return new Thread(() -> {
            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                this.convertToNdjsonStream(inputStream, writer);
            } catch (IOException e) {
                throw new RuntimeException("Error in writer thread", e);
            }
        });
    }



    private void convertToNdjsonStream(InputStream inputStream, Writer writer) throws IOException {
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
                    throw new IllegalArgumentException("Unsupported JSON format: expected array or NDJSON");
            }

            writer.flush();
        }



    private void writeArrayAsNdjson(JsonParser parser, ObjectMapper mapper, Writer writer) throws IOException {
        while (parser.nextToken() == JsonToken.START_OBJECT) {
            JsonNode node = mapper.readTree(parser);

            String jsonLine = mapper.writeValueAsString(node).replace("＼", "\\uFF3C").replace("\\", "\\\\");
            writer.write(jsonLine);
            writer.write('\n');
        }
    }

    private void writeNdjsonStream(JsonParser parser, ObjectMapper mapper, Writer writer) throws IOException {
        JsonNode node = mapper.readTree(parser);

        writer.write(mapper.writeValueAsString(node).replace("＼", "\\uFF3C").replace("\\", "\\\\"));
        writer.write('\n');

        while (parser.nextToken() == JsonToken.START_OBJECT) {
            node = mapper.readTree(parser);
            writer.write(mapper.writeValueAsString(node).replace("＼", "\\uFF3C").replace("\\", "\\\\"));
            writer.write('\n');
        }
    }

}

