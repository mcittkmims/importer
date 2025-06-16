package com.internship.importer.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JsonDataRecord {
    private Long id;
    private String rawJson;

}
