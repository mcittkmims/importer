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

    private final String uploadUrl;
    private final StagingRepository repository;
    private final ExecutorService executorService;
    private final RepositoryHelper repositoryHelper;

    private static final int BATCH_SIZE = 100;

    @Override
    public void sendStagingData(String companyJsonData, String industryJsonData,
                                String taxAuthorityJsonData, String taxInfoJsonData) {
        try (Stream<JsonDataRecord> stream = repository.getUnprocessedJsonDataStream()) {
            Iterator<JsonDataRecord> sourceIterator = stream.iterator();

            ContentBody companyBody = createJsonBody(companyJsonData);
            ContentBody industryBody = createJsonBody(industryJsonData);
            ContentBody taxAuthorityBody = createJsonBody(taxAuthorityJsonData);
            ContentBody taxInfoBody = createJsonBody(taxInfoJsonData);

            List<Future<?>> futures = new ArrayList<>();

            while (sourceIterator.hasNext()) {
                List<JsonDataRecord> batch = getNextBatch(sourceIterator);

                if (batch.isEmpty()) {
                    break;
                }

                futures.add(submitBatchProcessing(batch, companyBody, industryBody, taxAuthorityBody, taxInfoBody));
            }

            waitForCompletion(futures);
        } catch (Exception e) {
            throw new DataExportException("Export process failed", e);
        }
    }

    private ContentBody createJsonBody(String jsonData) {
        return new StringBody(jsonData, ContentType.APPLICATION_JSON);
    }

    private List<JsonDataRecord> getNextBatch(Iterator<JsonDataRecord> sourceIterator) {
        List<JsonDataRecord> batch = new ArrayList<>(BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE && sourceIterator.hasNext(); i++) {
            batch.add(sourceIterator.next());
        }
        return batch;
    }

    private Future<?> submitBatchProcessing(List<JsonDataRecord> batch,
                                            ContentBody companyBody,
                                            ContentBody industryBody,
                                            ContentBody taxAuthorityBody,
                                            ContentBody taxInfoBody) {
        return executorService.submit(() -> {
            try {
                HttpEntity entity = buildMultipartEntity(batch, companyBody, industryBody, taxAuthorityBody, taxInfoBody);
                executeHttpRequest(entity);
                markBatchAsProcessed(batch);
                log.info("Processed batch of {} records", batch.size());
            } catch (Exception e) {
                log.error("Failed to process batch", e);
                throw new DataExportException("Batch processing failed", e);
            }
        });
    }

    private HttpEntity buildMultipartEntity(List<JsonDataRecord> batch,
                                            ContentBody companyBody,
                                            ContentBody industryBody,
                                            ContentBody taxAuthorityBody,
                                            ContentBody taxInfoBody) {
        Iterator<String> jsonIterator = batch.stream()
                .map(r -> r.getRawJson() + "\n")
                .iterator();

        InputStreamBody dataBody = new InputStreamBody(
                new StreamingInputStream(jsonIterator),
                ContentType.APPLICATION_JSON,
                "data.ndjson"
        );

        return MultipartEntityBuilder.create()
                .addPart("companyMapping", companyBody)
                .addPart("industryMapping", industryBody)
                .addPart("taxAuthorityMapping", taxAuthorityBody)
                .addPart("taxInfoMapping", taxInfoBody)
                .addPart("data", dataBody)
                .build();
    }

    private void markBatchAsProcessed(List<JsonDataRecord> batch) {
        List<Long> ids = batch.stream().map(JsonDataRecord::getId).toList();
        repositoryHelper.markRowsWithTransaction(repository, ids);
    }

    private void waitForCompletion(List<Future<?>> futures) throws InterruptedException, ExecutionException {
        for (Future<?> future : futures) {
            future.get();
        }
        futures.clear();
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
