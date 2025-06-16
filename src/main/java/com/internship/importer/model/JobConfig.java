package com.internship.importer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class JobConfig {

    private JsonNode source;

    @JsonProperty("db_credentials")
    private DbCredentials dbCredentials;

    @JsonProperty("db_connection")
    private DbConnection dbConnection;

    private String table;

    @JsonProperty("export_url")
    private String exportUrl;

    private Mappings mappings;

    private String archived;

    @Data
    public static class DbCredentials {
        private String username;
        private String password;
    }

    @Data
    public static class DbConnection {
        private String jdbc;
    }

    @Data
    public static class Mappings {
        private JsonNode company;
        private JsonNode industry;
    }
}
