package com.internship.importer.service;

import com.internship.importer.config.ImportAppConfig;
import com.internship.importer.helper.IteratorInputStream;
import com.internship.importer.loader.DataLoader;
import com.internship.importer.mapper.CompanyMapper;
import lombok.AllArgsConstructor;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Iterator;


@Service
@AllArgsConstructor
public class CompanyService {
    private final CompanyMapper companyMapper;
    private JsonDownloadService downloadService;
    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);
    private ImportAppConfig config;
    private DataLoader dataLoader;
    private SqlSessionFactory sqlSessionFactory;

    public void importCompanyData(String url){
        downloadService.downloadFile(url,inputStream -> dataLoader.loadData(inputStream));
    }

    public void exportCompanyData(String url) throws IOException {
        HttpPost post = new HttpPost(url);

        String companyMappingJson = "{\n" +
                "    \"companyName\": \"$.name\",\n" +
                "    \"companyNumber\": \"$.corporate_number\",\n" +
                "    \"companyStatus\": \"$.status\",\n" +
                "    \"companyLocation\": \"$.location\",\n" +
                "    \"dateOfCreation\": \"$.date_of_establishment\",\n" +
                "    \"industries\": \"$.business_items\"\n" +
                "  }";
        ContentBody companyMappingBody = new StringBody(companyMappingJson, ContentType.APPLICATION_JSON);

        String industryMappingJson = "{\n" +
                "    \"industryCode\": \"$\"\n" +
                "  }";
        ContentBody industryMappingBody = new StringBody(industryMappingJson, ContentType.APPLICATION_JSON);

        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            Iterator<String> iterator = sqlSession.getMapper(CompanyMapper.class).getJsonData().iterator();

            IteratorInputStream iteratorInputStream = new IteratorInputStream(iterator);

            InputStreamBody dataBody = new InputStreamBody(iteratorInputStream, ContentType.APPLICATION_JSON, "data.ndjson");

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addPart("companyMapping", companyMappingBody)
                    .addPart("industryMapping", industryMappingBody)
                    .addPart("data", dataBody)
                    .build();

            post.setEntity(entity);

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(post)) {

                System.out.println(EntityUtils.toString(response.getEntity()));
            }
        }
    }


}
