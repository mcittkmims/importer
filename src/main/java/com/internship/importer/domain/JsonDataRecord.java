package com.internship.importer.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JsonDataRecord {
    private Long id;
    private String rawJson;

}
