package com.internship.importer.repository;

import com.internship.importer.domain.JsonDataRecord;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

@AllArgsConstructor
public class StagingRepository {
    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    public Stream<JsonDataRecord> getUnprocessedJsonDataStream() {
        if (!StagingTableService.isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name format.");
        }
        String sql = "SELECT id, raw_json FROM " + tableName + " WHERE exported = false ORDER BY id";
        jdbcTemplate.setFetchSize(100);
        return jdbcTemplate.queryForStream(
                sql,
                (rs, rowNum) -> new JsonDataRecord(
                        rs.getLong("id"),
                        rs.getString("raw_json")
                )
        );
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

    public DataSource getDataSource(){
        return jdbcTemplate.getDataSource();
    }




}


