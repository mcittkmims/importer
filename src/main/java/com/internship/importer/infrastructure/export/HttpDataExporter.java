package com.internship.importer.infrastructure.export;

import com.internship.importer.repository.StagingRepository;
import com.internship.importer.exception.DataExportException;
import com.internship.importer.exception.HttpRequestException;
import com.internship.importer.util.StreamingInputStream;
import com.internship.importer.domain.JsonDataRecord;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@AllArgsConstructor
@Slf4j
public class HttpDataExporter implements DataExporter {

    private String uploadUrl;
    private StagingRepository repository;
    private ExecutorService exportExecutorService;
    private StagingDataProcessor stagingDataProcessor;

    public void sendStagingData(String companyJsonData, String industryJsonData, String tableName) {
        int batchSize = 500;
        try {
            ContentBody companyMappingBody = this.createCompanyMappingBody(companyJsonData);
            ContentBody industryMappingBody = this.createIndustryMappingBody(industryJsonData);

            this.processBatchesWithProcessor(batchSize, companyMappingBody, industryMappingBody);

        } catch (IOException e) {
            throw new DataExportException("Failed to export company data to: " + uploadUrl, e);
        }
    }

    private void processBatchesWithProcessor(int batchSize,
            ContentBody companyMappingBody,
            ContentBody industryMappingBody) throws IOException {
        List<Future<Void>> futures = new ArrayList<>();
        int batchCount = 0;

        while (true) {
            AdvancedBoolean empty = new AdvancedBoolean();

            stagingDataProcessor.processBatch(repository, batchSize, records -> {
                if (records.isEmpty()) {
                    empty.setTrue();
                } else {
                    Future<Void> future = submitBatch(records, companyMappingBody, industryMappingBody);
                    futures.add(future);
                }
            });

            if (empty.getCondition()) {
                break;
            }

            batchCount++;
            log.info("Processed batch #{}", batchCount);
        }

        log.info("Completed processing {} batches", batchCount);
        waitForCompletion(futures);
    }

    private Future<Void> submitBatch(List<JsonDataRecord> jsonList,
            ContentBody companyMappingBody,
            ContentBody industryMappingBody) {

        return exportExecutorService.submit(() -> {
            try {
                sendBatch(jsonList, companyMappingBody, industryMappingBody);
                return null;
            } catch (IOException e) {
                log.error("Failed to send batch: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private void waitForCompletion(List<Future<Void>> futures) {
        for (Future<Void> future : futures) {
            try {
                future.get(30, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("Batch execution failed: {}", e.getMessage(), e);
                futures.forEach(f -> f.cancel(true));
                throw new DataExportException("Batch processing failed", e);
            }
        }
    }

    private void sendBatch(List<JsonDataRecord> jsonList,
            ContentBody companyMappingBody,
            ContentBody industryMappingBody) throws IOException {

        InputStreamBody dataBody = this.createDataBody(jsonList.stream().map(JsonDataRecord::getRawJson));
        HttpEntity entity = this.createMultipartEntity(companyMappingBody, industryMappingBody, dataBody);
        this.executeHttpRequest(entity);

    }

    private void executeHttpRequest(HttpEntity entity) throws IOException {
        HttpPost post = new HttpPost(uploadUrl);
        post.setEntity(entity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(post)) {

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 400) {
                throw new HttpRequestException("HTTP request failed with status: " + statusCode, statusCode);
            }

            String responseString = EntityUtils.toString(response.getEntity());
            log.info("Server response: {}", responseString);
        } catch (IOException e) {
            throw new HttpRequestException("Failed to execute HTTP request to: " + uploadUrl, e);
        }
    }

    private HttpEntity createMultipartEntity(ContentBody companyMappingBody, ContentBody industryMappingBody,
            InputStreamBody dataBody) {
        return MultipartEntityBuilder.create()
                .addPart("companyMapping", companyMappingBody)
                .addPart("industryMapping", industryMappingBody)
                .addPart("data", dataBody)
                .build();
    }

    private ContentBody createCompanyMappingBody(String companyMappingJson) throws IOException {
        return new StringBody(companyMappingJson, ContentType.APPLICATION_JSON);
    }

    private ContentBody createIndustryMappingBody(String industryMappingJson) throws IOException {
        return new StringBody(industryMappingJson, ContentType.APPLICATION_JSON);
    }

    private InputStreamBody createDataBody(Stream<String> jsonList) {
        Iterator<String> iterator = jsonList
                .map(s -> s + "\n")
                .iterator();

        StreamingInputStream streamingInputStream = new StreamingInputStream(iterator);
        return new InputStreamBody(streamingInputStream, ContentType.APPLICATION_JSON, "data.ndjson");
    }

}
