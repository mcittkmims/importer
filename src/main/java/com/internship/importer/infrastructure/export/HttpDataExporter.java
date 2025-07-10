package com.internship.importer.infrastructure.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.importer.domain.PartitionBatch;
import com.internship.importer.domain.ProcessingStatus;
import com.internship.importer.exception.DataExportException;
import com.internship.importer.exception.HttpRequestException;
import com.internship.importer.repository.StagingRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
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
                PartitionBatch partition = repository.fetchAndMarkNextPartition();
                if (partition == null) {
                    log.info("No more unexported partitions found.");
                    break;
                }
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

    private void executeHttpRequest(String jsonPayload) {
        HttpPost post = new HttpPost(uploadUrl);
        post.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30000)
                .setSocketTimeout(60000)
                .setConnectionRequestTimeout(30000)
                .build();
        post.setConfig(requestConfig);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
             CloseableHttpResponse response = httpClient.execute(post)) {

            int statusCode = response.getStatusLine().getStatusCode();
            String responseString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            if (statusCode >= 400) {
                String reasonPhrase = response.getStatusLine().getReasonPhrase();
                String message = String.format(
                        "HTTP request failed with status: %d %s. Server response: %s",
                        statusCode, reasonPhrase, responseString);
                throw new HttpRequestException(message, statusCode);
            }
        } catch (Exception e) {
            throw new DataExportException("HTTP request failed", e);
        }
    }
}