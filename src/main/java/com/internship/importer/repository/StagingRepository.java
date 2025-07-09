package com.internship.importer.repository;

import com.internship.importer.domain.JsonData;
import com.internship.importer.domain.JsonDataRecord;
import com.internship.importer.domain.PartitionBatch;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@AllArgsConstructor
public class StagingRepository {
    private final JdbcTemplate jdbcTemplate;
    private final String tableName;



    public List<Long> insertBatchJsonDataReturnIds(List<JsonData> batch) {
        if (!StagingTableService.isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name format.");
        }

        if (batch.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (raw_json, json_hash) VALUES ");

        List<Object> args = new ArrayList<>();

        for (int i = 0; i < batch.size(); i++) {
            sql.append("(?::jsonb, ?)");
            if (i < batch.size() - 1) {
                sql.append(", ");
            }

            JsonData jsonData = batch.get(i);
            args.add(jsonData.getRawJson());
            args.add(jsonData.getJsonHash());
        }

        sql.append(" ON CONFLICT (json_hash) DO NOTHING RETURNING id");

        return jdbcTemplate.query(
                sql.toString(),
                args.toArray(),
                (rs, rowNum) -> rs.getLong("id")
        );
    }

    public void insertPartitionBatches(List<PartitionBatch> batches) {
        String sql = "INSERT INTO company_partition (start_id, end_id, status) VALUES (?, ?, false)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PartitionBatch batch = batches.get(i);
                ps.setInt(1, batch.getStartId());
                ps.setInt(2, batch.getEndId());
            }

            @Override
            public int getBatchSize() {
                return batches.size();
            }
        });

    }

    public List<PartitionBatch> lockAndFetchPartitions(int limit) {
        String sql = "UPDATE company_partition " +
                "SET in_progress = true " +
                "WHERE id IN (SELECT id FROM company_partition " +
                "            WHERE sent_status = false AND in_progress = false " +
                "            FOR UPDATE SKIP LOCKED LIMIT ?) " +
                "RETURNING id, start_id, end_id, sent_status";

        return jdbcTemplate.query(sql, new Object[]{limit}, (rs, rowNum) -> new PartitionBatch(
                rs.getLong("id"),
                rs.getInt("start_id"),
                rs.getInt("end_id"),
                rs.getBoolean("sent_status")
        ));
    }

    public void markPartitionAsSent(long partitionId) {
        String sql = "UPDATE company_partition SET sent_status = true, in_progress = false WHERE id = ?";
        jdbcTemplate.update(sql, partitionId);
    }

    public void markPartitionAsFailed(long partitionId) {
        String sql = "UPDATE company_partition SET in_progress = false WHERE id = ?";
        jdbcTemplate.update(sql, partitionId);
    }

    public List<String> getRawJsonForPartition(int startId, int endId) {
        String sql = "SELECT raw_json FROM " + tableName + " WHERE id BETWEEN ? AND ?";
        return jdbcTemplate.query(sql, new Object[]{startId, endId}, (rs, rowNum) -> rs.getString("raw_json"));
    }


}


