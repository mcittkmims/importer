package com.internship.importer.sender;

import com.internship.importer.data.StagingDataAccessor;
import com.internship.importer.exception.DataExportException;
import com.internship.importer.exception.HttpRequestException;
import com.internship.importer.helper.IteratorInputStream;
import com.internship.importer.model.JsonDataRecord;
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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Slf4j
public class HttpDataSender implements DataSender {

    private String uploadUrl;
    private StagingDataAccessor accessor;


    public void sendStagingData(String companyJsonData, String industryJsonData, String tableName) {
        int batchSize = 500;
        try {
            ContentBody companyMappingBody = this.createCompanyMappingBody(companyJsonData);
            ContentBody industryMappingBody = this.createIndustryMappingBody(industryJsonData);

            while (true) {
                List<JsonDataRecord> jsonList = accessor.getBatchJsonData(tableName, batchSize);
                if (jsonList.isEmpty()) {
                    break;
                }
                InputStreamBody dataBody = this.createDataBody(jsonList.stream().map(JsonDataRecord::getRawJson));
                HttpEntity entity = this.createMultipartEntity(companyMappingBody, industryMappingBody, dataBody);
                this.executeHttpRequest(entity);
                accessor.deleteRowsByIds(tableName, jsonList.stream().map(JsonDataRecord::getId).collect(Collectors.toList()));
            }

        } catch (IOException e) {
            throw new DataExportException("Failed to export company data to: " + uploadUrl, e);
        }
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

    private HttpEntity createMultipartEntity(ContentBody companyMappingBody, ContentBody industryMappingBody, InputStreamBody dataBody) {
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

        IteratorInputStream iteratorInputStream = new IteratorInputStream(iterator);
        return new InputStreamBody(iteratorInputStream, ContentType.APPLICATION_JSON, "data.ndjson");
    }


}
