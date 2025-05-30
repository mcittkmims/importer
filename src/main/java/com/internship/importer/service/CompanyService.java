package com.internship.importer.service;

import com.internship.importer.config.ImportAppConfig;
import com.internship.importer.loader.DataLoader;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CompanyService {
    private JsonDownloadService downloadService;
    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);
    private ImportAppConfig config;
    private DataLoader dataLoader;

    public void importCompanyData(String url){
        downloadService.downloadFile(url,inputStream -> dataLoader.loadData(inputStream));
    }


}
