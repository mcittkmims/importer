package com.internship.importer.infrastructure.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.importer.domain.PartitionBatch;
import com.internship.importer.domain.ProcessingStatus;
import com.internship.importer.exception.DataExportException;
import com.internship.importer.repository.StagingRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@AllArgsConstructor
@Slf4j
public class HttpDataExporter implements DataExporter {
    private final String uploadUrl;
    private final String table;
    private final JdbcTemplate jdbcTemplate;
    private final ExecutorService executorService;

    @Override
    public void sendStagingData(String companyJsonData, String industryJsonData,
                                String taxAuthorityJsonData, String taxInfoJsonData) {
        try {
            StagingRepository repository = new StagingRepository(jdbcTemplate, table);

            while (true) {
                List<PartitionBatch> partitions = repository.fetchAndMarkNextPartition(100);
                if (partitions.isEmpty()) {
                    log.info("No more unexported partitions found.");
                    break;
                }

                for (PartitionBatch partition : partitions) {
                    executorService.submit(() -> {
                        try {
                            List<String> records = repository.selectJsonsByIdRange(partition.getStartId(), partition.getEndId());
                            String jsonPayload = buildJsonPayload(records, companyJsonData, industryJsonData, taxAuthorityJsonData, taxInfoJsonData);
                            executeHttpRequest(jsonPayload);

                            repository.updatePartitionProcessingStatus(partition.getStartId(), partition.getEndId(), ProcessingStatus.COMPLETED, true);

                            log.info("Exported partition: start_id={}, end_id={}", partition.getStartId(), partition.getEndId());
                        } catch (Exception e) {
                            repository.updatePartitionProcessingStatus(partition.getStartId(), partition.getEndId(), ProcessingStatus.FAILED, false);
                            log.error("Failed to export partition: start_id={}, end_id={}", partition.getStartId(), partition.getEndId(), e);
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Export process failed.", e);
            throw new DataExportException("Export process failed", e);
        }
    }

    private String buildJsonPayload(List<String> data,
                                    String companyJsonData,
                                    String industryJsonData,
                                    String taxAuthorityJsonData,
                                    String taxInfoJsonData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> payload = new HashMap<>();
            payload.put("data", data);
            payload.put("companyMapping", objectMapper.readValue(companyJsonData, Object.class));
            payload.put("industryMapping", objectMapper.readValue(industryJsonData, Object.class));
            payload.put("taxAuthorityMapping", objectMapper.readValue(taxAuthorityJsonData, Object.class));
            payload.put("taxInfoMapping", objectMapper.readValue(taxInfoJsonData, Object.class));
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {}
        return null;
    }

    private void executeHttpRequest(String jsonPayload) throws IOException {
        URL url = new URL(uploadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonPayload.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("POST request failed with status " + responseCode);
        }
    }
}