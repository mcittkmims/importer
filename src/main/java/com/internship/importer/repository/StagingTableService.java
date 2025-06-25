package com.internship.importer.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@AllArgsConstructor
public class StagingTableService {
    private final JdbcTemplate jdbcTemplate;

    public void createStagingTable(String tableName) {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name format.");
        }

        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "raw_json JSONB, " +
                "inserted_at TIMESTAMP DEFAULT NOW(), " +
                "exported BOOL DEFAULT FALSE" +
                ")";

        jdbcTemplate.execute(sql);
    }

    public static boolean isValidTableName(String tableName){
        return tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }
}

