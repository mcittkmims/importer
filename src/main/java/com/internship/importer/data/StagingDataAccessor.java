package com.internship.importer.data;

import com.internship.importer.model.JsonDataRecord;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

@AllArgsConstructor
public class StagingDataAccessor {
    private final JdbcTemplate jdbcTemplate;

    public List<JsonDataRecord> getBatchJsonData(String tableName, int limit) {
        if (!StagingTableManager.isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name format.");
        }

        String sql = "SELECT id, raw_json FROM " + tableName +
                " ORDER BY inserted_at " +
                " LIMIT ?";

        return jdbcTemplate.query(
                sql,
                new Object[] { limit },
                (rs, rowNum) -> new JsonDataRecord(
                        rs.getLong("id"),
                        rs.getString("raw_json")
                )
        );
    }

    public int deleteRowsByIds(String tableName, List<Long> ids) {
        if (!StagingTableManager.isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name format.");
        }
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        String sql = "DELETE FROM " + tableName + " WHERE id IN (:ids)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids);

        return namedJdbcTemplate.update(sql, params);
    }
}

