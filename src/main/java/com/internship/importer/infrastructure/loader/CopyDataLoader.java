package com.internship.importer.infrastructure.loader;

import com.internship.importer.exception.DataCopyException;
import com.internship.importer.exception.JsonParsingException;
import com.internship.importer.infrastructure.format.StreamConverter;
import com.internship.importer.repository.StagingTableService;
import lombok.AllArgsConstructor;
import org.postgresql.PGConnection;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@AllArgsConstructor
public class CopyDataLoader implements DataLoader {

    private final DataSource dataSource;
    private StreamConverter converter;

    @Override
    public void loadData(InputStream inputStream, String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);

            Thread writerThread = createWriterThread(inputStream, out);
            writerThread.start();

            Thread copyThread = new Thread(() -> {
                copyToDatabase(connection, in, tableName);
            });
            copyThread.start();

            writerThread.join();
            copyThread.join();
        } catch (SQLException e) {
            throw new DataCopyException("Failed to establish database connection for data loading", e);
        } catch (IOException e) {
            throw new DataCopyException("Failed to load data into database", e);
        } catch (InterruptedException e) {
            throw new DataCopyException("Failed to join threads", e);
        }
    }

    private void copyToDatabase(Connection connection, InputStream in, String tableName) {
        try (in; Statement stmt = connection.createStatement()) {

            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            if (!StagingTableService.isValidTableName(tableName)) {
                throw new IllegalArgumentException("Invalid table name format.");
            }
            pgConnection.getCopyAPI().copyIn("COPY " + tableName + " (raw_json) FROM STDIN", in);

        } catch (SQLException e) {
            throw new DataCopyException("Error during database COPY operation", e);
        } catch (IOException e) {
            throw new DataCopyException("Error during data stream processing", e);
        }
    }

    private Thread createWriterThread(InputStream inputStream, OutputStream out) {
        return new Thread(() -> {
            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                this.converter.convertInputStream(inputStream, writer);
            } catch (IOException e) {
                throw new JsonParsingException("Error in input stream conversion thread", e);
            }
        });
    }

}
