package com.internship.importer.repository;

import com.internship.importer.domain.JsonDataRecord;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

@AllArgsConstructor
public class StagingRepository {
    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    public List<JsonDataRecord> getBatchJsonData(int limit) {
        if (!StagingTableService.isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name format.");
        }

        String sql = "SELECT id, raw_json FROM " + tableName +
                " WHERE exported = false" +
                //" ORDER BY inserted_at" +
                " FOR UPDATE SKIP LOCKED LIMIT ?";

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new JsonDataRecord(
                        rs.getLong("id"),
                        rs.getString("raw_json")),
                limit);  
    }

    public int markRowsByIds(List<Long> ids) {
        if (!StagingTableService.isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name format.");
        }
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        String sql = "UPDATE " + tableName + " SET exported = true WHERE id IN (:ids)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids);

        return namedJdbcTemplate.update(sql, params);
    }
}
