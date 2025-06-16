package com.internship.importer.sender;

public interface DataSender {
    void sendStagingData(String companyJsonData, String industryJsonData, String tableName);
}
