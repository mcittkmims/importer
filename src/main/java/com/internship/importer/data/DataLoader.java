package com.internship.importer.data;

import com.internship.importer.exception.DataCopyException;
import com.internship.importer.exception.JsonParsingException;
import com.internship.importer.format.InputStreamConverter;
import com.internship.importer.format.InputStreamToNdjsonConverter;
import lombok.AllArgsConstructor;
import org.postgresql.PGConnection;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@AllArgsConstructor
public class DataLoader {

    private final DataSource dataSource;
    private InputStreamConverter converter;

    public void loadData(InputStream inputStream, String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);

            Thread writerThread = createWriterThread(inputStream, out);
            writerThread.start();

            copyToDatabase(connection, in, tableName);
        } catch (SQLException e) {
            throw new DataCopyException("Failed to establish database connection for data loading", e);
        } catch (IOException e) {
            throw new DataCopyException("Failed to load data into database", e);
        }
    }

    private void copyToDatabase(Connection connection, InputStream in, String tableName) {
        try (in; Statement stmt = connection.createStatement()) {

            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            if(!StagingTableManager.isValidTableName(tableName)){
                throw new IllegalArgumentException("Invalid table name format.");
            }
            pgConnection.getCopyAPI().copyIn("COPY "+ tableName +" (raw_json) FROM STDIN", in);

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

