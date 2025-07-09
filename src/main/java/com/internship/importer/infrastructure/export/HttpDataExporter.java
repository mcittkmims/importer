package com.internship.importer.infrastructure.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.importer.domain.PartitionBatch;
import com.internship.importer.exception.DataExportException;
import com.internship.importer.exception.HttpRequestException;
import com.internship.importer.repository.PartitionRepository;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@AllArgsConstructor
@Slf4j
public class HttpDataExporter implements DataExporter {

    private final String uploadUrl;
    private final PartitionRepository partitionRepository;
    private final ExecutorService executorService;
    private final PartitionManager partitionManager;

    @Override
    public void sendStagingData(String companyJsonData, String industryJsonData,
            String taxAuthorityJsonData, String taxInfoJsonData) {
//        try {
//            long overallStartTime = System.currentTimeMillis();
//
//            partitionRepository.resetStuckProcessingBatches();
//
//            partitionRepository.resetFailedBatches();
//
//            int partitionsCreated = partitionManager.addPartitionsForNewData();
//            log.info("Partition management completed. {} partitions created. {}",
//                    partitionsCreated, partitionManager.getPartitionInfo());
//
//            if (!partitionRepository.hasUnprocessedBatches()) {
//                log.info("No unprocessed partitions found. Nothing to export.");
//                return;
//            }
//
//            processAllPartitions(companyJsonData, industryJsonData, taxAuthorityJsonData, taxInfoJsonData);
//
//            long overallElapsed = System.currentTimeMillis() - overallStartTime;
//            log.info("Completed processing all partitions in {} ms. Final status: {}",
//                    overallElapsed, partitionRepository.getDetailedPartitionStatus());
//        } catch (Exception e) {
//            log.error("Export process failed. Current status: {}", partitionRepository.getDetailedPartitionStatus(), e);
//
//        }
    }
//
//    private void processAllPartitions(String companyJsonData, String industryJsonData,
//            String taxAuthorityJsonData, String taxInfoJsonData)
//            throws InterruptedException, ExecutionException {
//        List<Future<?>> futures = new ArrayList<>();
//        final int maxConcurrentTasks = ((ThreadPoolExecutor) executorService).getMaximumPoolSize();
//
//        boolean hasMoreBatches = true;
//        long startTime = System.currentTimeMillis();
//
//        while (hasMoreBatches) {
//            int availableSlots = maxConcurrentTasks - futures.size();
//            if (availableSlots <= 0) {
//                // Wait for some tasks to complete before fetching more
//                waitForSomeTasks(futures, futures.size() / 2);
//                continue;
//            }
//
//            List<PartitionBatch> batches = partitionRepository.getNextUnprocessedBatches(availableSlots);
//
//            if (batches.isEmpty()) {
//                hasMoreBatches = false;
//                break;
//            }
//
//            int submitted = 0;
//            for (PartitionBatch batch : batches) {
//                try {
//
//                    Future<?> future = submitPartitionProcessing(batch, companyJsonData, industryJsonData,
//                            taxAuthorityJsonData, taxInfoJsonData);
//                    futures.add(future);
//                    submitted++;
//                } catch (Exception e) {
//                }
//            }
//
//            long elapsed = System.currentTimeMillis() - startTime;
//            if (elapsed > 120000) // 2 minutes
//            {
//                List<Long> currentlyProcessing = partitionRepository.getProcessingBatchIds();
//
//                if (currentlyProcessing.size() > futures.size() * 1.2) {
//                    log.warn("Detected potential stuck batches: {} processing in DB but only {} active futures. " +
//                            "This suggests some threads failed to start or are stuck.",
//                            currentlyProcessing.size(), futures.size());
//                }
//
//                startTime = System.currentTimeMillis(); // Reset timer
//            }
//        }
//
//        waitForCompletion(futures);
//
//        List<Long> processingAfterWait = partitionRepository.getProcessingBatchIds();
//        if (!processingAfterWait.isEmpty()) {
//            log.warn("Resetting {} remaining PROCESSING batches back to PENDING", processingAfterWait.size());
//            int resetCount = partitionRepository.emergencyResetAllProcessingBatches();
//            log.warn("Reset {} PROCESSING batches back to PENDING after thread completion", resetCount);
//        }
//    }
//
//    private Future<?> submitPartitionProcessing(PartitionBatch batch,
//            String companyJsonData,
//            String industryJsonData,
//            String taxAuthorityJsonData,
//            String taxInfoJsonData) {
//
//        if (executorService.isShutdown()) {
//            throw new IllegalStateException("ExecutorService is shut down");
//        }
//
//        try {
//            Future<?> future = executorService.submit(() -> {
//                long startTime = System.currentTimeMillis();
//
//                try {
//                    if (Thread.currentThread().isInterrupted()) {
//                        throw new InterruptedException("Thread interrupted before processing");
//                    }
//
//                    if (!partitionRepository.tryMarkBatchAsProcessing(batch)) {
//                        return;
//                    }
//
//                    List<String> batchData = partitionRepository.getDataForBatch(batch);
//
//                    if (!batchData.isEmpty()) {
//                        String jsonPayload = buildJsonPayload(batchData, companyJsonData, industryJsonData,
//                                taxAuthorityJsonData, taxInfoJsonData);
//                        executeHttpRequest(jsonPayload);
//
//                        partitionRepository.markBatchAsCompleted(batch);
//
//                        long elapsed = System.currentTimeMillis() - startTime;
//                        log.info("Processed partition batch {} with {} records (IDs {}-{}) in {} ms",
//                                batch.getId(), batchData.size(), batch.getStartId(), batch.getEndId(), elapsed);
//                    } else {
//                        partitionRepository.markBatchAsCompleted(batch);
//                        long elapsed = System.currentTimeMillis() - startTime;
//                        log.info("Partition batch {} had no records, completed in {} ms", batch.getId(), elapsed);
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt(); // Restore interrupted status
//                    try {
//                        partitionRepository.markBatchAsFailed(batch);
//                    } catch (Exception markFailedException) {
//                    }
//                    return;
//                } catch (Exception e) {
//                    long elapsed = System.currentTimeMillis() - startTime;
//                    log.error("Failed to process partition batch {} after {} ms", batch.getId(), elapsed, e);
//
//
//                    try {
//                        partitionRepository.markBatchAsFailed(batch);
//                    } catch (Exception markFailedException) {
//                    }
//
//                    throw new DataExportException("Partition batch processing failed", e);
//                }
//            });
//
//            return future;
//
//        } catch (Exception submitException) {
//            throw new DataExportException("Failed to submit batch for processing", submitException);
//        }
//    }
//
//    private void waitForSomeTasks(List<Future<?>> futures, int tasksToWait)
//            throws InterruptedException, ExecutionException {
//        int completed = 0;
//        Iterator<Future<?>> iterator = futures.iterator();
//        final long timeoutPerTask = 120000;
//
//        while (iterator.hasNext() && completed < tasksToWait) {
//            Future<?> future = iterator.next();
//            try {
//
//                future.get(timeoutPerTask, TimeUnit.MILLISECONDS);
//                iterator.remove();
//                completed++;
//            } catch (TimeoutException e) {
//                future.cancel(true); // Interrupt the thread
//                iterator.remove();
//                completed++; // Count cancelled tasks as completed to avoid infinite loop
//            } catch (ExecutionException e) {
//                iterator.remove();
//                completed++;
//                throw e;
//            }
//        }
//    }
//
//    private String buildJsonPayload(List<String> jsonDataList,
//            String companyJsonData,
//            String industryJsonData,
//            String taxAuthorityJsonData,
//            String taxInfoJsonData) {
//
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//
//            Object companyMapping = objectMapper.readValue(companyJsonData, Object.class);
//            Object industryMapping = objectMapper.readValue(industryJsonData, Object.class);
//            Object taxAuthorityMapping = objectMapper.readValue(taxAuthorityJsonData, Object.class);
//            Object taxInfoMapping = objectMapper.readValue(taxInfoJsonData, Object.class);
//
//            Map<String, Object> payload = new HashMap<>();
//            payload.put("data", jsonDataList);
//            payload.put("companyMapping", companyMapping);
//            payload.put("industryMapping", industryMapping);
//            payload.put("taxAuthorityMapping", taxAuthorityMapping);
//            payload.put("taxInfoMapping", taxInfoMapping);
//
//            String jsonPayload = objectMapper.writeValueAsString(payload);
//
//            return jsonPayload;
//
//        } catch (Exception e) {
//        }
//        return null;
//    }
//
//    private void waitForCompletion(List<Future<?>> futures) throws InterruptedException, ExecutionException {
//        final long timeoutPerTask = 120000;
//        Iterator<Future<?>> iterator = futures.iterator();
//        int completedCount = 0;
//        int timeoutCount = 0;
//        int totalTasks = futures.size();
//
//        while (iterator.hasNext()) {
//            Future<?> future = iterator.next();
//            try {
//                future.get(timeoutPerTask, TimeUnit.MILLISECONDS);
//                completedCount++;
//            } catch (TimeoutException e) {
//                timeoutCount++;
//                future.cancel(true);
//            } catch (ExecutionException e) {
//                throw e;
//            }
//            iterator.remove();
//        }
//
//        futures.clear();
//    }
//
//    private void executeHttpRequest(String jsonPayload) throws IOException {
//        HttpPost post = new HttpPost(uploadUrl);
//
//        StringEntity entity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
//        post.setEntity(entity);
//
//        RequestConfig requestConfig = RequestConfig.custom()
//                .setConnectTimeout(30000)
//                .setSocketTimeout(60000)
//                .setConnectionRequestTimeout(30000)
//                .build();
//        post.setConfig(requestConfig);
//
//        try (CloseableHttpClient httpClient = HttpClients.custom()
//                .setDefaultRequestConfig(requestConfig)
//                .build();
//                CloseableHttpResponse response = httpClient.execute(post)) {
//
//            int statusCode = response.getStatusLine().getStatusCode();
//            String responseString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
//
//            if (statusCode >= 400) {
//                String reasonPhrase = response.getStatusLine().getReasonPhrase();
//                String message = String.format(
//                        "HTTP request failed with status: %d %s. Server response: %s",
//                        statusCode, reasonPhrase, responseString);
//                throw new HttpRequestException(message, statusCode);
//            }
//
//        } catch (Exception e) {
//        }
//    }
}
