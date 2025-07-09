package com.internship.importer.repository;

import com.internship.importer.domain.PartitionBatch;
import com.internship.importer.domain.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Slf4j
public class PartitionRepository {
    private final JdbcTemplate jdbcTemplate;
    private final String stagingTableName;


//    public int createPartitionsForNewData(int batchSize) {
//        if (!StagingTableService.isValidTableName(stagingTableName)) {
//            throw new IllegalArgumentException("Invalid table name format.");
//        }
//        String checkColumnSql = "SELECT COUNT(*) FROM information_schema.columns " +
//                "WHERE table_schema = 'public' AND table_name = ? AND column_name = 'partitioned'";
//        Integer columnCount = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, stagingTableName);
//        if (columnCount == 0) {
//            return 0;
//        }
//        String unpartitionedCountSql = "SELECT COUNT(*) FROM " + stagingTableName + " WHERE partitioned = false";
//        Long unpartitionedRecords = jdbcTemplate.queryForObject(unpartitionedCountSql, Long.class);
//        if (unpartitionedRecords == 0) {
//            return 0;
//        }
//        String sql = "SELECT MIN(id), MAX(id) FROM " + stagingTableName + " WHERE partitioned = false";
//        return jdbcTemplate.query(sql, rs -> {
//            if (rs.next()) {
//                Long minId = rs.getObject(1, Long.class);
//                Long maxId = rs.getObject(2, Long.class);
//                if (minId != null && maxId != null) {
//                    return createPartitionBatches(minId, maxId, batchSize);
//                } else {
//                    return 0;
//                }
//            }
//            return 0;
//        });
//    }
//
//    private int createPartitionBatches(long minId, long maxId, int batchSize) {
//        String insertSql = "INSERT INTO company_partition (start_id, end_id, status, processing_status) VALUES (?, ?, false, 'PENDING')";
//        int batchCount = 0;
//        long currentStart = minId;
//        while (currentStart <= maxId) {
//            long currentEnd = Math.min(currentStart + batchSize - 1, maxId);
//            jdbcTemplate.update(insertSql, currentStart, currentEnd);
//            batchCount++;
//            currentStart = currentEnd + 1;
//        }
//        markRecordsAsPartitioned(minId, maxId);
//        return batchCount;
//    }
//
//    private void markRecordsAsPartitioned(long minId, long maxId) {
//        if (!StagingTableService.isValidTableName(stagingTableName)) {
//            throw new IllegalArgumentException("Invalid table name format.");
//        }
//        String updateSql = "UPDATE " + stagingTableName + " SET partitioned = true WHERE id BETWEEN ? AND ?";
//        jdbcTemplate.update(updateSql, minId, maxId);
//    }
//
//    public Optional<PartitionBatch> getNextUnprocessedBatch() {
//        String sql = "SELECT id, start_id, end_id, status, processing_status FROM company_partition " +
//                "WHERE status = false AND processing_status = 'PENDING' ORDER BY id LIMIT 1";
//        return jdbcTemplate.query(sql, rs -> {
//            if (rs.next()) {
//                return Optional.of(new PartitionBatch(
//                        rs.getLong("id"),
//                        rs.getInt("start_id"),
//                        rs.getInt("end_id"),
//                        rs.getBoolean("status"),
//                        ProcessingStatus.valueOf(rs.getString("processing_status"))));
//            }
//            return Optional.<PartitionBatch>empty();
//        });
//    }
//
//    public List<PartitionBatch> getNextUnprocessedBatches(int limit) {
//        String selectSql = "SELECT id, start_id, end_id, status, processing_status " +
//                "FROM company_partition " +
//                "WHERE status = false AND processing_status = 'PENDING' " +
//                "ORDER BY id LIMIT ?";
//        return jdbcTemplate.query(selectSql,
//                (rs, rowNum) -> new PartitionBatch(
//                        rs.getLong("id"),
//                        rs.getInt("start_id"),
//                        rs.getInt("end_id"),
//                        rs.getBoolean("status"),
//                        ProcessingStatus.valueOf(rs.getString("processing_status"))),
//                limit);
//    }
//
//    public boolean tryMarkBatchAsProcessing(PartitionBatch batch) {
//        String updateSql = "UPDATE company_partition SET processing_status = 'PROCESSING' " +
//                "WHERE id = ? AND processing_status = 'PENDING'";
//        int updatedRows = jdbcTemplate.update(updateSql, batch.getId());
//        return updatedRows > 0;
//    }
//
//    public List<String> getDataForBatch(PartitionBatch batch) {
//        if (!StagingTableService.isValidTableName(stagingTableName)) {
//            throw new IllegalArgumentException("Invalid table name format.");
//        }
//        String sql = "SELECT raw_json FROM " + stagingTableName +
//                " WHERE id BETWEEN ? AND ? ORDER BY id";
//        return jdbcTemplate.query(sql,
//                (rs, rowNum) -> rs.getString("raw_json"),
//                batch.getStartId(), batch.getEndId());
//    }
//
//    public void markBatchAsCompleted(PartitionBatch batch) {
//        String sql = "UPDATE company_partition SET status = true, processing_status = 'COMPLETED' WHERE id = ?";
//        jdbcTemplate.update(sql, batch.getId());
//    }
//
//    public void markBatchAsFailed(PartitionBatch batch) {
//        String sql = "UPDATE company_partition SET processing_status = 'FAILED' WHERE id = ?";
//        jdbcTemplate.update(sql, batch.getId());
//    }
//
//    public void resetFailedBatches() {
//        String sql = "UPDATE company_partition SET processing_status = 'PENDING' WHERE processing_status = 'FAILED'";
//        jdbcTemplate.update(sql);
//    }
//
//    public void resetStuckProcessingBatches() {
//        int processingCount = countProcessingBatches();
//        if (processingCount == 0) {
//            return;
//        }
//        int maxToReset = Math.min(processingCount, 50);
//        String sql = "UPDATE company_partition SET processing_status = 'PENDING' " +
//                "WHERE id IN (SELECT id FROM company_partition " +
//                "WHERE processing_status = 'PROCESSING' " +
//                "ORDER BY id LIMIT ?)";
//        jdbcTemplate.update(sql, maxToReset);
//    }
//
//    public boolean hasUnprocessedBatches() {
//        String sql = "SELECT EXISTS(SELECT 1 FROM company_partition WHERE status = false AND processing_status IN ('PENDING', 'FAILED'))";
//        return jdbcTemplate.queryForObject(sql, Boolean.class);
//    }
//
//    public DataSource getDataSource() {
//        return jdbcTemplate.getDataSource();
//    }
//
//    public int countFailedBatches() {
//        String sql = "SELECT COUNT(*) FROM company_partition WHERE processing_status = 'FAILED'";
//        return jdbcTemplate.queryForObject(sql, Integer.class);
//    }
//
//    public int countPendingBatches() {
//        String sql = "SELECT COUNT(*) FROM company_partition WHERE processing_status = 'PENDING'";
//        return jdbcTemplate.queryForObject(sql, Integer.class);
//    }
//
//    public int countProcessingBatches() {
//        String sql = "SELECT COUNT(*) FROM company_partition WHERE processing_status = 'PROCESSING'";
//        return jdbcTemplate.queryForObject(sql, Integer.class);
//    }
//
//    public String getDetailedPartitionStatus() {
//        int total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM company_partition", Integer.class);
//        int completed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM company_partition WHERE status = true",
//                Integer.class);
//        int pending = countPendingBatches();
//        int processing = countProcessingBatches();
//        int failed = countFailedBatches();
//        return String.format("Partition Status - Total: %d, Completed: %d, Pending: %d, Processing: %d, Failed: %d",
//                total, completed, pending, processing, failed);
//    }
//
//    public List<Long> getProcessingBatchIds() {
//        String sql = "SELECT id FROM company_partition WHERE processing_status = 'PROCESSING' ORDER BY id";
//        return jdbcTemplate.queryForList(sql, Long.class);
//    }
//
//    public int emergencyResetAllProcessingBatches() {
//        String sql = "UPDATE company_partition SET processing_status = 'PENDING' WHERE processing_status = 'PROCESSING'";
//        return jdbcTemplate.update(sql);
//    }
}
