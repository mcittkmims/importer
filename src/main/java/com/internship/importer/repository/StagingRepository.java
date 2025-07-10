package com.internship.importer.repository;

import com.internship.importer.domain.JsonData;
import com.internship.importer.domain.PartitionBatch;

import com.internship.importer.domain.ProcessingStatus;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public List<String> selectJsonsByIdRange(long startId, long endId) {
        String sql = "SELECT raw_json FROM " + tableName + " WHERE id BETWEEN ? AND ?";
        return jdbcTemplate.query(
                sql,
                new Object[]{startId, endId},
                (rs, rowNum) -> rs.getString("raw_json")
        );
    }

    public PartitionBatch findFirstUnexportedPartition() {
        String sql = "SELECT start_id, end_id FROM company_partition WHERE status = false ORDER BY start_id LIMIT 1";
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return new PartitionBatch(rs.getInt("start_id"), rs.getInt("end_id"));
            }
            return null;
        });
    }

    public void updatePartitionStatus(long startId, long endId, boolean status) {
        String sql = "UPDATE company_partition SET status = ? WHERE start_id = ? AND end_id = ?";
        jdbcTemplate.update(sql, status, startId, endId);
    }

    public PartitionBatch fetchAndMarkNextPartition() {
        String sql = "UPDATE company_partition " +
                "SET processing_status = 'PROCESSING' " +
                "WHERE id = (" +
                "  SELECT id FROM company_partition " +
                "  WHERE processing_status = 'PENDING' " +
                "  ORDER BY start_id " +
                "  LIMIT 1 " +
                "  FOR UPDATE SKIP LOCKED" +
                ") " +
                "RETURNING start_id, end_id";

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return new PartitionBatch(rs.getInt("start_id"), rs.getInt("end_id"));
            }
            return null;
        });
    }

    public void updatePartitionProcessingStatus(long startId, long endId, ProcessingStatus status, boolean exported) {
        String sql = "UPDATE company_partition " +
                "SET processing_status = ?, status = ? " +
                "WHERE start_id = ? AND end_id = ?";
        jdbcTemplate.update(sql, status.name(), exported, startId, endId);
    }

}


