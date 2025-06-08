package com.internship.importer.service;

import com.internship.importer.exception.DataExportException;
import com.internship.importer.exception.DataImportException;
import com.internship.importer.exception.DatabaseException;
import com.internship.importer.exception.FileResourceException;
import com.internship.importer.exception.HttpRequestException;
import com.internship.importer.helper.IteratorInputStream;
import com.internship.importer.loader.DataLoader;
import com.internship.importer.mapper.CompanyMapper;
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
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Iterator;


@Service
public class CompanyService {
    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);
    private SqlSessionFactory sqlSessionFactory;
    private JsonDownloadService downloadService;
    private DataLoader dataLoader;
    private ResourceLoader resourceLoader;


    public CompanyService(JsonDownloadService downloadService, DataLoader dataLoader, ResourceLoader resourceLoader, SqlSessionFactory sqlSessionFactory) {
        this.downloadService = downloadService;
        this.dataLoader = dataLoader;
        this.resourceLoader = resourceLoader;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Value("${importer.download.http.method:POST}")
    private String httpMethod;
    @Value("${importer.download.url}")
    private String downloadUrl;
    @Value("${importer.upload.url}")
    private String uploadUrl;

    @Value("${importer.mapping.company.file}")
    private String companyMappingFile;

    @Value("${importer.mapping.industry.file}")
    private String industryMappingFile;

    public void importCompanyData() {
        try {
            downloadService.downloadFile(downloadUrl, httpMethod, inputStream -> dataLoader.loadData(inputStream));
        } catch (Exception e) {
            throw new DataImportException("Failed to import company data from: " + downloadUrl, e);
        }
    }

    public void exportCompanyData() {
        try {
            ContentBody companyMappingBody = this.createCompanyMappingBody();
            ContentBody industryMappingBody = this.createIndustryMappingBody();

            try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                InputStreamBody dataBody = this.createDataBody(sqlSession);
                HttpEntity entity = this.createMultipartEntity(companyMappingBody, industryMappingBody, dataBody);
                this.executeHttpRequest(entity);
            }
        } catch (IOException e) {
            throw new DataExportException("Failed to export company data to: " + uploadUrl, e);
        }
    }


    private void executeHttpRequest(HttpEntity entity) throws IOException{
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

    private ContentBody createCompanyMappingBody() throws IOException {
        try {
            String companyMappingJson = StreamUtils.copyToString(
                    resourceLoader.getResource(companyMappingFile).getInputStream(),
                    StandardCharsets.UTF_8);
            return new StringBody(companyMappingJson, ContentType.APPLICATION_JSON);
        } catch (IOException e) {
            throw new FileResourceException("Failed to load company mapping file: " + companyMappingFile, e);
        }
    }

    private ContentBody createIndustryMappingBody() throws IOException {
        try {
            String industryMappingJson = StreamUtils.copyToString(
                    resourceLoader.getResource(industryMappingFile).getInputStream(),
                    StandardCharsets.UTF_8);
            return new StringBody(industryMappingJson, ContentType.APPLICATION_JSON);
        } catch (IOException e) {
            throw new FileResourceException("Failed to load industry mapping file: " + industryMappingFile, e);
        }
    }

    private InputStreamBody createDataBody(SqlSession sqlSession) {
        Iterator<String> iterator = sqlSession.getMapper(CompanyMapper.class).getJsonData().iterator();
        IteratorInputStream iteratorInputStream = new IteratorInputStream(iterator);
        return new InputStreamBody(iteratorInputStream, ContentType.APPLICATION_JSON, "data.ndjson");
    }


}



