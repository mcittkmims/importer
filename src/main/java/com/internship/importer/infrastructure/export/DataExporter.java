package com.internship.importer.infrastructure.export;

public interface DataExporter {
    void sendStagingData(String companyJsonData, String industryJsonData);
}
