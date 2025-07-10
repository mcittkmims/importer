package com.internship.importer.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@AllArgsConstructor
public class StagingTableService {
    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    public void createStagingTable() {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name format.");
        }

        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "raw_json JSONB NOT NULL, " +
                "json_hash TEXT UNIQUE NOT NULL, " +
                "inserted_at TIMESTAMP DEFAULT NOW()" +
                ")";


        jdbcTemplate.execute(sql);

    }

    public void createPartitionTable() {

        String sql = "CREATE TABLE IF NOT EXISTS company_partition (\n" +
                "            id BIGSERIAL PRIMARY KEY,\n" +
                "            start_id INT,\n" +
                "            end_id INT,\n" +
                "            status BOOLEAN,\n" +
                "            processing_status VARCHAR(20) DEFAULT 'PENDING'\n" +
                "    );";

        jdbcTemplate.execute(sql);
    }

    public static boolean isValidTableName(String tableName) {
        return tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

}
