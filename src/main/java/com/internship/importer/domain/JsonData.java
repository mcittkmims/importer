package com.internship.importer.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JsonData {
    private String rawJson;
    private String jsonHash;
}
