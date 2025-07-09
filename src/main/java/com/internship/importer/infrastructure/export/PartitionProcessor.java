package com.internship.importer.infrastructure.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.importer.domain.PartitionBatch;
import com.internship.importer.repository.StagingRepository;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class PartitionProcessor implements Runnable {
    private final PartitionBatch partition;
    private final StagingRepository stagingRepository;
    private final String postUrl;
    private final String companyJsonData;
    private final String industryJsonData;
    private final String taxAuthorityJsonData;
    private final String taxInfoJsonData;

    @Override
    public void run() {
        try {
            List<String> rawJsonList = stagingRepository.getRawJsonForPartition(
                    partition.getStartId(),
                    partition.getEndId()
            );
            if (!rawJsonList.isEmpty()) {
                sendJsonBatch(rawJsonList);
                stagingRepository.markPartitionAsSent(partition.getId());
            }
        } catch (Exception e) {
            System.err.println("Error processing partition " + partition.getId() + ": " + e.getMessage());
            stagingRepository.markPartitionAsFailed(partition.getId());
        }
    }

    private void sendJsonBatch(List<String> jsonList) throws IOException {
        URL url = new URL(postUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String joinedJson = buildJsonPayload(jsonList);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(joinedJson.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("POST request failed with status " + responseCode);
        }
    }

    @SneakyThrows
    private String buildJsonPayload(List<String> jsonDataList) {

        ObjectMapper objectMapper = new ObjectMapper();

        Object companyMapping = objectMapper.readValue(companyJsonData, Object.class);
        Object industryMapping = objectMapper.readValue(industryJsonData, Object.class);
        Object taxAuthorityMapping = objectMapper.readValue(taxAuthorityJsonData, Object.class);
        Object taxInfoMapping = objectMapper.readValue(taxInfoJsonData, Object.class);

        Map<String, Object> payload = new HashMap<>();
        payload.put("data", jsonDataList);
        payload.put("companyMapping", companyMapping);
        payload.put("industryMapping", industryMapping);
        payload.put("taxAuthorityMapping", taxAuthorityMapping);
        payload.put("taxInfoMapping", taxInfoMapping);

        String jsonPayload = objectMapper.writeValueAsString(payload);

        return jsonPayload;

    }
}
