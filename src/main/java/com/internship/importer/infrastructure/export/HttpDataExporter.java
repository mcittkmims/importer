package com.internship.importer.infrastructure.export;

import com.internship.importer.domain.JsonDataRecord;
import com.internship.importer.exception.DataExportException;
import com.internship.importer.exception.HttpRequestException;
import com.internship.importer.repository.RepositoryHelper;
import com.internship.importer.repository.StagingRepository;
import com.internship.importer.util.StreamingInputStream;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@AllArgsConstructor
@Slf4j
public class HttpDataExporter implements DataExporter {

    private String uploadUrl;
    private StagingRepository repository;
    private ExecutorService executorService;
    private RepositoryHelper repositoryHelper;



    public void sendStagingData(String companyJsonData, String industryJsonData,
                                String taxAuthorityJsonData, String taxInfoJsonData) {
        int batchSize = 100;
        try (Stream<JsonDataRecord> stream = repository.getUnprocessedJsonDataStream()) {
            Iterator<JsonDataRecord> sourceIterator = stream.iterator();
            System.out.println("hey");
            ContentBody companyBody = new StringBody(companyJsonData, ContentType.APPLICATION_JSON);
            ContentBody industryBody = new StringBody(industryJsonData, ContentType.APPLICATION_JSON);
            ContentBody taxAuthorityBody = new StringBody(taxAuthorityJsonData, ContentType.APPLICATION_JSON);
            ContentBody taxInfoBody = new StringBody(taxInfoJsonData, ContentType.APPLICATION_JSON);

            List<Future<?>> futures = new ArrayList<>();

            // This executorService should be provided/injected, reused across calls
            // We just submit batch processing to it

            while (sourceIterator.hasNext()) {
                List<JsonDataRecord> batch = new ArrayList<>(batchSize);

                for (int i = 0; i < batchSize && sourceIterator.hasNext(); i++) {
                    batch.add(sourceIterator.next());
                }

                if (batch.isEmpty()) {
                    break;
                }

                futures.add(executorService.submit(() -> {
                    try {
                        Iterator<String> jsonIterator = batch.stream()
                                .map(r -> r.getRawJson() + "\n")
                                .iterator();

                        InputStreamBody dataBody = new InputStreamBody(
                                new StreamingInputStream(jsonIterator),
                                ContentType.APPLICATION_JSON,
                                "data.ndjson"
                        );

                        HttpEntity entity = MultipartEntityBuilder.create()
                                .addPart("companyMapping", companyBody)
                                .addPart("industryMapping", industryBody)
                                .addPart("taxAuthorityMapping", taxAuthorityBody)
                                .addPart("taxInfoMapping", taxInfoBody)
                                .addPart("data", dataBody)
                                .build();

                        executeHttpRequest(entity);

                        List<Long> ids = batch.stream().map(JsonDataRecord::getId).toList();
                        repositoryHelper.markRowsWithTransaction(repository, ids);

                        log.info("Processed batch of {} records", batch.size());
                    } catch (Exception e) {
                        log.error("Failed to process batch", e);
                        throw new DataExportException("Batch processing failed", e);
                    }
                }));
            }

            for (Future<?> future : futures) {
                future.get();
            }
            futures.clear();
        } catch (Exception e) {
            throw new DataExportException("Export process failed", e);
        }
    }



    private void executeHttpRequest(HttpEntity entity) throws IOException {
        HttpPost post = new HttpPost(uploadUrl);
        post.setEntity(entity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {

            int statusCode = response.getStatusLine().getStatusCode();
            String responseString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            if (statusCode >= 400) {
                String reasonPhrase = response.getStatusLine().getReasonPhrase();
                String message = String.format(
                        "HTTP request failed with status: %d %s. Server response: %s",
                        statusCode, reasonPhrase, responseString
                );
                throw new HttpRequestException(message, statusCode);
            }

            log.info("Server response: {}", responseString);
        }
    }

}
